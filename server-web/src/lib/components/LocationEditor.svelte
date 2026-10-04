<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { GroupController, UserController } from '$lib/api';
	import type {
		ApiFieldError,
		LocationResponse,
		LocationTypeResponse,
		SimpleAuthProviderResponse,
		UserResponse,
	} from '$lib/api/types.gen';
	import { auditHeaders, getApiClient } from '$lib/api-client';
	import AuditSaveCard from '$lib/components/AuditSaveCard.svelte';
	import ObjectPropertyForm from '$lib/components/ObjectPropertyForm.svelte';
	import type { SchemaFormError } from '$lib/components/SchemaForm.svelte';
	import PageContent from '$lib/components/layout/PageContent.svelte';
	import { t, tApiErrorResponse } from '$lib/i18n/catalog';
	import { locationTypeName } from '$lib/i18n/labels';
	import {
		compactPropertyValues,
		filterCreateFields,
		seedCreateValues,
		toTypePropertyAssignments,
		valuesFromExistingProperties,
	} from '$lib/properties/createFields';
	import toast from 'svelte-hot-french-toast';

	let {
		mode,
		types,
		authProviders,
		auditRequired,
		location,
		user,
		loadError,
		kind = $bindable<'user' | 'group' | undefined>(),
	}: {
		mode: 'create' | 'edit';
		types: LocationTypeResponse[];
		authProviders: SimpleAuthProviderResponse[];
		auditRequired: boolean;
		location?: LocationResponse;
		user?: UserResponse;
		loadError?: ApiFieldError;
		kind?: 'user' | 'group' | undefined;
	} = $props();

	const formId = 'location-editor-form';

	type SelectableLocationType = LocationTypeResponse & { kind: 'user' | 'group' };

	function isSelectableType(type: LocationTypeResponse): type is SelectableLocationType {
		return type.kind === 'user' || type.kind === 'group';
	}

	const isEdit = $derived(mode === 'edit');

	const selectableTypes = $derived(
		types
			.filter(isSelectableType)
			.toSorted((a, b) => locationTypeName(a.id).localeCompare(locationTypeName(b.id)))
	);

	function fieldsForType(forTypeId: string) {
		const type = types.find(entry => entry.id === forTypeId);
		return filterCreateFields(toTypePropertyAssignments(type?.properties), {
			omitLocationIdentity: true,
		});
	}

	function resolveKind(forTypeId: string): 'user' | 'group' | undefined {
		if (mode === 'edit' && location?.kind === 'user') {
			return 'user';
		}
		if (mode === 'edit' && location?.kind === 'group') {
			return 'group';
		}
		const selected = types.find(entry => entry.id === forTypeId);
		return selected?.kind === 'user' || selected?.kind === 'group' ? selected.kind : undefined;
	}

	function initialTypeId() {
		if (mode === 'edit' && location) {
			return location.type;
		}
		return types
			.filter(isSelectableType)
			.toSorted((a, b) => locationTypeName(a.id).localeCompare(locationTypeName(b.id)))
			.at(0)?.id ?? '';
	}

	function initialUsername() {
		if (mode === 'edit') {
			return user?.username ?? location?.name ?? '';
		}
		return '';
	}

	function initialName() {
		if (mode === 'edit') {
			return location?.name ?? '';
		}
		return '';
	}

	function initialAuthProviderId() {
		if (mode === 'edit') {
			return user?.authProvider ?? '';
		}
		return '';
	}

	function initialEnabled() {
		if (mode === 'edit') {
			return user?.enabled ?? true;
		}
		return true;
	}

	function initialValues(typeId: string) {
		const fields = fieldsForType(typeId);
		if (mode === 'edit' && location) {
			return valuesFromExistingProperties(fields, location.properties);
		}
		return typeId ? seedCreateValues(fields) : {};
	}

	const startingTypeId = initialTypeId();
	let typeId = $state(startingTypeId);
	let username = $state(initialUsername());
	let name = $state(initialName());
	let authProviderId = $state(initialAuthProviderId());
	let enabled = $state(initialEnabled());
	let values = $state<Record<string, unknown>>(initialValues(startingTypeId));
	let formError = $state<SchemaFormError>();
	let auditComment = $state('');
	let submitting = $state(false);

	kind = resolveKind(startingTypeId);

	function snapshot() {
		return {
			typeId,
			username,
			name,
			authProviderId,
			enabled,
			values: $state.snapshot(values),
			kind,
		};
	}

	let baseline = $state(snapshot());

	const selectedType = $derived(
		isEdit
			? types.find(entry => entry.id === typeId)
			: selectableTypes.find(entry => entry.id === typeId)
	);
	const fields = $derived(fieldsForType(typeId));

	function resetForType(nextTypeId: string) {
		username = '';
		name = '';
		authProviderId = '';
		enabled = true;
		formError = undefined;
		values = seedCreateValues(fieldsForType(nextTypeId));
		kind = resolveKind(nextTypeId);
	}

	function reset() {
		if (isEdit && location) {
			void goto(resolve('/(authenticated)/locations/[id]', { id: location.id }));
			return;
		}

		typeId = baseline.typeId;
		username = baseline.username;
		name = baseline.name;
		authProviderId = baseline.authProviderId;
		enabled = baseline.enabled;
		values = $state.snapshot(baseline.values);
		kind = baseline.kind;
		formError = undefined;
	}

	async function handleSubmit(event: SubmitEvent) {
		event.preventDefault();

		if (!kind) {
			formError = { error: 'web.locations.select_type' };
			return;
		}
		if (mode === 'create' && !typeId) {
			formError = { error: 'web.locations.select_type' };
			return;
		}
		if (kind === 'user' && !username) {
			return;
		}
		if (kind === 'group' && !name) {
			return;
		}

		submitting = true;
		formError = undefined;
		const properties = compactPropertyValues(values);

		if (mode === 'create') {
			if (kind === 'user') {
				const { data, error } = await UserController.create({
					client: getApiClient(),
					body: {
						type: typeId,
						username,
						authProvider: authProviderId || undefined,
						properties,
					},
					headers: auditHeaders(auditComment),
				});

				submitting = false;

				if (error) {
					formError = error;
					return;
				}

				toast.success(t('web.locations.created_user'));
				const createdId = data?.data?.id;
				if (createdId) {
					await goto(resolve('/(authenticated)/locations/[id]', { id: createdId }));
				}
				return;
			}

			const { data, error } = await GroupController.createGroup({
				client: getApiClient(),
				body: {
					type: typeId,
					name,
					properties,
				},
				headers: auditHeaders(auditComment),
			});

			submitting = false;

			if (error) {
				formError = error;
				return;
			}

			toast.success(t('web.locations.created_group'));
			const createdId = data?.data?.id;
			if (createdId) {
				await goto(resolve('/(authenticated)/locations/[id]', { id: createdId }));
			}
			return;
		}

		if (!location) {
			submitting = false;
			return;
		}

		if (kind === 'user') {
			const { error } = await UserController.update({
				client: getApiClient(),
				path: { id: location.id },
				body: {
					username,
					authProvider: authProviderId || undefined,
					enabled,
					properties,
				},
				headers: auditHeaders(auditComment),
			});

			submitting = false;

			if (error) {
				formError = error;
				return;
			}

			toast.success(t('web.locations.updated_user'));
			await goto(resolve('/(authenticated)/locations/[id]', { id: location.id }));
			return;
		}

		const { error } = await GroupController.updateGroup({
			client: getApiClient(),
			path: { id: location.id },
			body: {
				name,
				properties,
			},
			headers: auditHeaders(auditComment),
		});

		submitting = false;

		if (error) {
			formError = error;
			return;
		}

		toast.success(t('web.locations.updated_group'));
		await goto(resolve('/(authenticated)/locations/[id]', { id: location.id }));
	}
</script>

<PageContent>
	{#if isEdit}
		<h1 class="mb-6 text-2xl font-semibold">{t('web.locations.edit_title')}</h1>
	{:else}
		<h1 class="mb-6 text-2xl font-semibold">{t('web.locations.create_title')}</h1>
	{/if}

	{#if loadError}
		<p class="text-destructive">{tApiErrorResponse(loadError)}</p>
	{:else if isEdit && !location}
		<p class="text-destructive">{t('web.locations.not_found')}</p>
	{:else if !isEdit && selectableTypes.length === 0}
		<p class="text-hint">{t('web.locations.no_types')}</p>
	{:else}
		<form id={formId} class="card p-5" onsubmit={handleSubmit}>
			{#if isEdit}
				<div class="mb-4 flex flex-col gap-1">
					<span class="text-label">{t('web.common.type')}</span>
					<p class="text-sm">
						{locationTypeName(typeId)}
						{#if kind}
							({kind === 'user' ? t('web.locations.kind_user') : t('web.locations.kind_group')})
						{/if}
					</p>
				</div>
			{:else}
				<label class="mb-4 flex flex-col gap-1">
					<span class="text-label">{t('web.common.type')}</span>
					<select
						class="input w-full"
						bind:value={typeId}
						disabled={submitting}
						onchange={() => resetForType(typeId)}
					>
						{#each selectableTypes as type (type.id)}
							<option value={type.id}>
								{locationTypeName(type.id)} ({type.kind === 'user'
									? t('web.locations.kind_user')
									: t('web.locations.kind_group')})
							</option>
						{/each}
					</select>
				</label>
			{/if}

			{#if kind === 'user'}
				<label class="mb-4 flex flex-col gap-1">
					<span class="text-label">{t('web.locations.username')}</span>
					<input class="input w-full" bind:value={username} required disabled={submitting} />
				</label>

				<label class="mb-4 flex flex-col gap-1">
					<span class="text-label">{t('web.locations.auth_provider')}</span>
					<select class="input w-full" bind:value={authProviderId} disabled={submitting}>
						<option value="">{t('web.locations.auth_provider_none')}</option>
						{#each authProviders as provider (provider.id)}
							<option value={provider.id}>{provider.name}</option>
						{/each}
					</select>
				</label>

				{#if isEdit}
					<label class="mb-4 flex items-center gap-2">
						<input type="checkbox" bind:checked={enabled} disabled={submitting} />
						<span class="text-label">{t('web.locations.enabled')}</span>
					</label>
				{/if}
			{:else if kind === 'group'}
				<label class="mb-4 flex flex-col gap-1">
					<span class="text-label">{t('web.locations.name')}</span>
					<input class="input w-full" bind:value={name} required disabled={submitting} />
				</label>
			{/if}

			{#if selectedType || (isEdit && typeId)}
				{#key typeId}
					<ObjectPropertyForm {fields} bind:values={values} error={formError} {submitting} />
				{/key}
			{/if}
		</form>

		<AuditSaveCard
			form={formId}
			bind:auditComment
			required={auditRequired}
			error={formError}
			{submitting}
			dirty={true}
			onreset={reset}
		/>
	{/if}
</PageContent>
