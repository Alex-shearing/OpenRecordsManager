<script lang="ts">
	import {
		ObjectPropertyController,
		type ObjectPropertyResponse,
		type SimpleObjectPropertyResponse,
	} from '#lib/api/index.js';
	import { auditHeaders, getApiClient } from '#lib/api-client.js';
	import AdminCreateForm from '#lib/components/AdminCreateForm.svelte';
	import AdminFormError from '#lib/components/AdminFormError.svelte';
	import AdminPageIntro from '#lib/components/AdminPageIntro.svelte';
	import AuditCommentField from '#lib/components/AuditCommentField.svelte';
	import DialogActions from '#lib/components/DialogActions.svelte';
	import MonoId from '#lib/components/MonoId.svelte';
	import TableCard from '#lib/components/TableCard.svelte';
	import TargetDialog from '#lib/components/TargetDialog.svelte';
	import TypeEditButton from '#lib/components/TypeEditButton.svelte';
	import type { SchemaFormError } from '#lib/components/SchemaForm.svelte';
	import { t, tApiErrorResponse } from '#lib/i18n/catalog.js';
	import { objectPropertyDescription, objectPropertyName } from '#lib/i18n/labels.js';
	import { isBuiltinResourceId, PROPERTY_TYPES, type PropertyTypeName } from '#lib/types/typeAdmin.js';
	import { refreshAll } from '$app/navigation';
	import toast from 'svelte-hot-french-toast';

	let { data } = $props();

	const sorted = $derived([...data.properties].sort((a, b) => a.id.localeCompare(b.id)));

	let createId = $state('');
	let createName = $state('');
	let createDescription = $state('');
	let createType = $state<PropertyTypeName>('string');
	let createListType = $state('');
	let createValidator = $state('');
	let createSecurityFilter = $state('');
	let createDefault = $state('');
	let createAuditComment = $state('');
	let createError = $state<SchemaFormError>();
	let submitting = $state(false);

	let editTarget = $state<ObjectPropertyResponse>();
	let editName = $state('');
	let editDescription = $state('');
	let editValidator = $state('');
	let editSecurityFilter = $state('');
	let editDefault = $state('');
	let editAuditComment = $state('');
	let editError = $state<SchemaFormError>();
	let editLoading = $state(false);

	const needsListType = $derived(createType === 'list_item' || createType === 'list_multiple');

	function parseDefault(raw: string): unknown {
		const trimmed = raw.trim();
		if (!trimmed) return undefined;
		try {
			return JSON.parse(trimmed);
		} catch {
			return trimmed;
		}
	}

	function formatDefault(value: unknown): string {
		if (value === undefined || value === null) return '';
		return typeof value === 'string' ? value : JSON.stringify(value);
	}

	async function handleCreate(event: SubmitEvent) {
		event.preventDefault();
		submitting = true;
		createError = undefined;

		const { error } = await ObjectPropertyController.objectPropertyCreate({
			client: getApiClient(),
			body: {
				id: createId.trim(),
				name: createName.trim(),
				description: createDescription.trim(),
				// OpenAPI models PropertyType as an object; the API expects the type name string.
				type: createType as unknown as { name?: string },
				listType: needsListType && createListType.trim() ? createListType.trim() : undefined,
				validator: createValidator.trim() || undefined,
				securityFilter: createSecurityFilter.trim() || undefined,
				defaultValue: parseDefault(createDefault),
			},
			headers: auditHeaders(createAuditComment),
		});

		submitting = false;
		if (error) {
			createError = error;
			return;
		}

		toast.success(t('web.object_properties.created'));
		createId = '';
		createName = '';
		createDescription = '';
		createType = 'string';
		createListType = '';
		createValidator = '';
		createSecurityFilter = '';
		createDefault = '';
		createAuditComment = '';
		await refreshAll();
	}

	async function openEdit(property: SimpleObjectPropertyResponse) {
		if (isBuiltinResourceId(property.id)) return;
		editLoading = true;
		editError = undefined;
		editAuditComment = '';

		const { data: result, error } = await ObjectPropertyController.objectPropertyRetrieveOne({
			client: getApiClient(),
			path: { id: property.id },
		});
		editLoading = false;

		if (error) {
			toast.error(tApiErrorResponse(error));
			return;
		}

		editTarget = result.data;
		editName = objectPropertyName(result.data.id);
		editDescription = objectPropertyDescription(result.data.id);
		editValidator = result.data.validator ?? '';
		editSecurityFilter = result.data.securityFilter ?? '';
		editDefault = formatDefault(result.data.defaultValue);
	}

	async function handleEdit(event: SubmitEvent) {
		event.preventDefault();
		if (!editTarget) return;
		submitting = true;
		editError = undefined;

		const { error } = await ObjectPropertyController.objectPropertyUpdate({
			client: getApiClient(),
			path: { id: editTarget.id },
			body: {
				name: editName.trim(),
				description: editDescription.trim(),
				validator: editValidator.trim() || undefined,
				securityFilter: editSecurityFilter.trim() || undefined,
				defaultValue: parseDefault(editDefault),
			},
			headers: auditHeaders(editAuditComment),
		});

		submitting = false;
		if (error) {
			editError = error;
			return;
		}

		toast.success(t('web.object_properties.updated'));
		editTarget = undefined;
		await refreshAll();
	}
</script>

<AdminPageIntro title={t('web.object_properties.title')} intro={t('web.object_properties.intro')} error={data.error} />

{#if !data.error}
	<TableCard
		title={t('web.object_properties.table_title')}
		items={sorted}
		empty={t('web.object_properties.empty')}
		getKey={p => p.id}
	>
		{#snippet header()}
			<th class="px-5 py-3 font-medium">{t('web.common.id')}</th>
			<th class="px-5 py-3 font-medium">{t('web.common.name')}</th>
			<th class="px-5 py-3 font-medium">{t('web.common.type')}</th>
			<th class="px-5 py-3 font-medium"><span class="sr-only">{t('web.common.actions')}</span></th>
		{/snippet}
		{#snippet row(property)}
			<td class="px-5 py-4"><MonoId value={property.id} /></td>
			<td class="px-5 py-4">{objectPropertyName(property.id)}</td>
			<td class="px-5 py-4">{property.type}</td>
			<td class="px-5 py-4 text-right">
				<TypeEditButton id={property.id} disabled={submitting || editLoading} onclick={() => openEdit(property)} />
			</td>
		{/snippet}
	</TableCard>

	<AdminCreateForm
		title={t('web.object_properties.create_title')}
		hint={t('web.object_properties.create_hint')}
		onsubmit={handleCreate}
		{submitting}
		submitDisabled={!createId.trim() || !createName.trim()}
		error={createError}
		bind:auditComment={createAuditComment}
		auditRequired={data.auditCommentRequired.create}
	>
		<label class="mb-4 flex flex-col gap-1">
			<span class="text-label">{t('web.common.id')}</span>
			<input
				class="input w-full font-mono"
				name="id"
				bind:value={createId}
				placeholder="custom:my_field"
				required
				disabled={submitting}
			/>
		</label>
		<label class="mb-4 flex flex-col gap-1">
			<span class="text-label">{t('web.common.name')}</span>
			<input class="input w-full" name="name" bind:value={createName} required disabled={submitting} />
		</label>
		<label class="mb-4 flex flex-col gap-1">
			<span class="text-label">{t('web.common.description')}</span>
			<textarea
				class="input w-full"
				name="description"
				bind:value={createDescription}
				required
				rows={2}
				disabled={submitting}></textarea>
		</label>
		<label class="mb-4 flex flex-col gap-1">
			<span class="text-label">{t('web.common.type')}</span>
			<select class="input w-full" name="type" bind:value={createType} disabled={submitting}>
				{#each PROPERTY_TYPES as type (type)}
					<option value={type}>{type}</option>
				{/each}
			</select>
		</label>
		{#if needsListType}
			<label class="mb-4 flex flex-col gap-1">
				<span class="text-label">{t('web.object_properties.list_type')}</span>
				<input
					class="input w-full font-mono"
					name="listType"
					bind:value={createListType}
					placeholder="custom:my_list"
					required
					disabled={submitting}
				/>
			</label>
		{/if}
		<label class="mb-4 flex flex-col gap-1">
			<span class="text-label">{t('web.object_properties.validator')}</span>
			<input class="input w-full font-mono" name="validator" bind:value={createValidator} disabled={submitting} />
		</label>
		<label class="mb-4 flex flex-col gap-1">
			<span class="text-label">{t('web.object_properties.security_filter')}</span>
			<textarea
				class="input w-full font-mono"
				name="securityFilter"
				bind:value={createSecurityFilter}
				rows={2}
				disabled={submitting}></textarea>
		</label>
		<label class="mb-4 flex flex-col gap-1">
			<span class="text-label">{t('web.object_properties.default_value')}</span>
			<input
				class="input w-full font-mono"
				name="defaultValue"
				bind:value={createDefault}
				placeholder={t('web.types.property_default_placeholder')}
				disabled={submitting}
			/>
		</label>
	</AdminCreateForm>
{/if}

<TargetDialog bind:target={editTarget} title="web.object_properties.edit_title" size="wide">
	{#snippet description(target)}
		{t('web.object_properties.edit_description', target.id)}
	{/snippet}
	{#snippet body(target)}
		<form id="object-property-edit-form" class="flex flex-col gap-4" onsubmit={handleEdit}>
			<label class="flex flex-col gap-1">
				<span class="text-label">{t('web.common.type')}</span>
				<input class="input w-full" name="type" value={target.type} readonly />
			</label>
			<label class="flex flex-col gap-1">
				<span class="text-label">{t('web.common.name')}</span>
				<input class="input w-full" name="name" bind:value={editName} required disabled={submitting} />
			</label>
			<label class="flex flex-col gap-1">
				<span class="text-label">{t('web.common.description')}</span>
				<textarea
					class="input w-full"
					name="description"
					bind:value={editDescription}
					required
					rows={2}
					disabled={submitting}></textarea>
			</label>
			<label class="flex flex-col gap-1">
				<span class="text-label">{t('web.object_properties.validator')}</span>
				<input class="input w-full font-mono" name="validator" bind:value={editValidator} disabled={submitting} />
			</label>
			<label class="flex flex-col gap-1">
				<span class="text-label">{t('web.object_properties.security_filter')}</span>
				<textarea
					class="input w-full font-mono"
					name="securityFilter"
					bind:value={editSecurityFilter}
					rows={2}
					disabled={submitting}></textarea>
			</label>
			<label class="flex flex-col gap-1">
				<span class="text-label">{t('web.object_properties.default_value')}</span>
				<input class="input w-full font-mono" name="defaultValue" bind:value={editDefault} disabled={submitting} />
			</label>
			<AdminFormError error={editError} />
			<AuditCommentField
				bind:value={editAuditComment}
				required={data.auditCommentRequired.update}
				disabled={submitting}
			/>
		</form>
	{/snippet}
	{#snippet footer(_target)}
		<DialogActions
			formId="object-property-edit-form"
			confirmLabel="web.common.save"
			confirmingLabel="web.common.saving"
			{submitting}
		/>
	{/snippet}
</TargetDialog>
