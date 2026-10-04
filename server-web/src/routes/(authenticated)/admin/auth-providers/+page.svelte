<script lang="ts">
	import { AuthController } from '#lib/api/index.js';
	import type { AuthProviderResponse, SimpleAuthProviderResponse } from '#lib/api/types.gen.js';
	import { auditHeaders, getApiClient } from '#lib/api-client.js';
	import DialogActions from '#lib/components/DialogActions.svelte';
	import MonoId from '#lib/components/MonoId.svelte';
	import SchemaForm from '#lib/components/SchemaForm.svelte';
	import { type SchemaFormError } from '#lib/components/SchemaForm.svelte';
	import TableCard from '#lib/components/TableCard.svelte';
	import TargetDialog from '#lib/components/TargetDialog.svelte';
	import { t, tApiErrorResponse } from '#lib/i18n/catalog.js';
	import { invalidateAll } from '$app/navigation';
	import toast from 'svelte-hot-french-toast';

	let { data } = $props();

	const sortedProviders = $derived(
		[...data.providers].sort((a, b) => {
			const nameCmp = (a.name ?? '').localeCompare(b.name ?? '');
			return nameCmp !== 0 ? nameCmp : (a.id ?? '').localeCompare(b.id ?? '');
		})
	);
	const sortedTypes = $derived([...data.types].sort((a, b) => a.type.id.localeCompare(b.type.id)));

	// svelte-ignore state_referenced_locally
	let createTypeId = $state(data.types.at(0)?.type.id ?? '');
	let createName = $state('');
	let createValues = $state<Record<string, string>>({});
	let createError = $state<SchemaFormError>();
	let createAuditComment = $state('');
	let submitting = $state(false);

	const createType = $derived(sortedTypes.find(type => type.type.id === createTypeId));

	let editTarget = $state<AuthProviderResponse>();
	let editName = $state('');
	let editEnabled = $state(true);
	let editValues = $state<Record<string, string>>({});
	let editError = $state<SchemaFormError>();
	let editAuditComment = $state('');
	let editLoading = $state(false);

	const editType = $derived(editTarget ? sortedTypes.find(type => type.type.id === editTarget?.type.id) : undefined);

	function toFormValues(properties: Record<string, unknown> | undefined): Record<string, string> {
		return Object.fromEntries(
			Object.entries(properties ?? {}).map(([key, value]) => [key, value == null ? '' : String(value)])
		);
	}

	async function handleCreate(event: SubmitEvent) {
		event.preventDefault();
		if (!createType) {
			createError = { error: 'web.auth_providers.select_type' };
			return;
		}

		submitting = true;
		createError = undefined;

		const { data: result, error } = await AuthController.createAuthProvider({
			client: getApiClient(),
			body: {
				name: createName,
				type: createType.type,
				settings: createType.settingsSchema ? createValues : {},
			},
			headers: auditHeaders(createAuditComment),
		});

		submitting = false;

		if (error) {
			createError = error;
			return;
		}

		toast.success(t('web.auth_providers.created'));

		const created = result?.data;
		if (createType.type.type === 'redirect_auth_provider' && created?.id) {
			toast.success(t('web.auth_providers.callback_url', `${window.location.origin}/api/auth/callback/${created.id}`), {
				duration: 10000,
			});
		}

		createName = '';
		createValues = {};
		createError = undefined;
		createAuditComment = '';

		await invalidateAll();
	}

	async function openEdit(provider: SimpleAuthProviderResponse) {
		editLoading = true;
		editValues = {};
		editError = undefined;
		editAuditComment = '';

		const { data: result, error } = await AuthController.getAuthProvider({
			client: getApiClient(),
			path: { id: provider.id },
		});

		editLoading = false;

		if (error) {
			toast.error(tApiErrorResponse(error));
			return;
		}

		editTarget = result.data;
		editName = result.data.name;
		editEnabled = result.data.enabled;
		editValues = toFormValues(result.data.settings);
	}

	async function handleEdit(event: SubmitEvent) {
		event.preventDefault();
		if (!editTarget) return;

		submitting = true;
		editError = undefined;

		const { error } = await AuthController.updateAuthProvider({
			client: getApiClient(),
			path: { id: editTarget.id },
			body: {
				name: editName,
				enabled: editEnabled,
				settings: editType?.settingsSchema ? editValues : undefined,
			},
			headers: auditHeaders(editAuditComment),
		});

		submitting = false;

		if (error) {
			editError = error;
			return;
		}

		toast.success(t('web.auth_providers.updated'));
		editTarget = undefined;
		await invalidateAll();
	}
</script>

<h1 class="mb-2 text-2xl font-semibold">{t('web.auth_providers.title')}</h1>
<p class="mb-6 text-hint">{t('web.auth_providers.intro')}</p>

{#if data.error}
	<p class="text-destructive">{tApiErrorResponse(data.error)}</p>
{:else}
	<TableCard
		title={t('web.auth_providers.table_title')}
		items={sortedProviders}
		empty={t('web.auth_providers.empty')}
		getKey={p => p.id}
	>
		{#snippet header()}
			<th class="px-5 py-3 font-medium">{t('web.common.name')}</th>
			<th class="px-5 py-3 font-medium">{t('web.common.type')}</th>
			<th class="px-5 py-3 font-medium">{t('web.common.enabled')}</th>
			<th class="px-5 py-3 font-medium">{t('web.common.id')}</th>
			<th class="px-5 py-3 font-medium"><span class="sr-only">{t('web.common.actions')}</span></th>
		{/snippet}
		{#snippet row(provider)}
			<td class="px-5 py-4 font-medium">{provider.name}</td>
			<td class="px-5 py-4">{provider.type.id}</td>
			<td class="px-5 py-4">{provider.enabled ? t('web.common.yes') : t('web.common.no')}</td>
			<td class="px-5 py-4">
				<MonoId value={provider.id} />
			</td>
			<td class="px-5 py-4 text-right">
				<button type="button" class="btn-ghost" disabled={submitting || editLoading} onclick={() => openEdit(provider)}>
					{t('web.common.edit')}
				</button>
			</td>
		{/snippet}
	</TableCard>

	<form class="mt-6 card p-5" onsubmit={handleCreate}>
		<h2 class="mb-1 text-lg font-medium">{t('web.auth_providers.create_title')}</h2>
		<p class="mb-4 text-hint">{t('web.auth_providers.create_hint')}</p>

		{#if sortedTypes.length === 0}
			<p class="text-hint">{t('web.auth_providers.no_types')}</p>
		{:else}
			<label class="mb-4 flex flex-col gap-1">
				<span class="text-label">{t('web.common.name')}</span>
				<input class="input w-full" bind:value={createName} required disabled={submitting} />
			</label>

			<label class="mb-4 flex flex-col gap-1">
				<span class="text-label">{t('web.common.type')}</span>
				<select
					class="input w-full"
					bind:value={createTypeId}
					disabled={submitting}
					onchange={() => {
						createValues = {};
						createError = undefined;
					}}
				>
					{#each sortedTypes as type (type.type.id)}
						<option value={type.type.id}>{type.type.id}</option>
					{/each}
				</select>
			</label>

			{#if createType}
				{#if createType.settingsSchema}
					{#key createTypeId}
						<SchemaForm
							schema={createType.settingsSchema}
							bind:values={createValues}
							error={createError}
							{submitting}
							idPrefix="auth-provider-create"
						>
							{#snippet after()}
								<label class="flex flex-col gap-1">
									<span class="text-label">{t('web.common.audit_comment')}</span>
									<textarea
										bind:value={createAuditComment}
										required={data.auditCommentRequired.create}
										disabled={submitting}
										rows={3}
										class="input w-full"></textarea>
								</label>
							{/snippet}
						</SchemaForm>
					{/key}
				{:else}
					<p class="mb-4 text-hint">{t('web.auth_providers.no_settings')}</p>
					{#if createError}
						<p class="mb-4 text-sm text-destructive" role="alert">{tApiErrorResponse(createError)}</p>
					{/if}
					<label class="mb-4 flex flex-col gap-1">
						<span class="text-label">{t('web.common.audit_comment')}</span>
						<textarea
							bind:value={createAuditComment}
							required={data.auditCommentRequired.create}
							disabled={submitting}
							rows={3}
							class="input w-full"></textarea>
					</label>
				{/if}
			{/if}

			<div class="mt-4">
				<button type="submit" class="btn-primary" disabled={submitting || !createTypeId || !createName}>
					{submitting ? t('web.common.creating') : t('web.common.create')}
				</button>
			</div>
		{/if}
	</form>
{/if}

<TargetDialog bind:target={editTarget} title="web.auth_providers.edit_title" size="wide">
	{#snippet description(target)}
		{t('web.auth_providers.edit_description', target.id)}
	{/snippet}
	{#snippet body(target)}
		<form id="auth-provider-edit-form" class="flex flex-col gap-4" onsubmit={handleEdit}>
			<label class="flex flex-col gap-1">
				<span class="text-label">{t('web.common.name')}</span>
				<input class="input w-full" bind:value={editName} required disabled={submitting} />
			</label>

			<label class="flex items-center gap-2">
				<input type="checkbox" bind:checked={editEnabled} disabled={submitting} />
				<span class="text-label">{t('web.common.enabled')}</span>
			</label>

			<label class="flex flex-col gap-1">
				<span class="text-label">{t('web.common.type')}</span>
				<input class="input w-full" value={target.type.id} readonly disabled />
			</label>

			{#if editType?.settingsSchema}
				{#key target.id}
					<SchemaForm
						schema={editType.settingsSchema}
						bind:values={editValues}
						error={editError}
						{submitting}
						idPrefix="auth-provider-edit"
					></SchemaForm>
				{/key}
			{:else}
				<p class="text-hint">{t('web.auth_providers.no_settings')}</p>
				{#if editError}
					<p class="text-sm text-destructive" role="alert">{tApiErrorResponse(editError)}</p>
				{/if}
			{/if}

			<label class="flex flex-col gap-1">
				<span class="text-label">{t('web.common.audit_comment')}</span>
				<textarea
					bind:value={editAuditComment}
					required={data.auditCommentRequired.update}
					disabled={submitting}
					rows={3}
					class="input w-full"></textarea>
			</label>
		</form>
	{/snippet}
	{#snippet footer()}
		<DialogActions
			formId="auth-provider-edit-form"
			confirmLabel="web.common.save"
			confirmingLabel="web.common.saving"
			{submitting}
		/>
	{/snippet}
</TargetDialog>
