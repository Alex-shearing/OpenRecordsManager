<script lang="ts">
	import { RelationshipTypeController, type RelationshipTypeResponse } from '#lib/api/index.js';
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
	import { t } from '#lib/i18n/catalog.js';
	import { isBuiltinResourceId, sortById } from '#lib/types/typeAdmin.js';
	import { refreshAll } from '$app/navigation';
	import toast from 'svelte-hot-french-toast';

	let { data } = $props();

	const sortedItems = $derived(sortById(data.types));
	const allHolderTypes = $derived(sortById([...data.availableLocationTypes, ...data.availableRecordTypes]));

	let submitting = $state(false);

	let createId = $state('');
	let createSourceTypeId = $state('');
	let createTargetTypeId = $state('');
	let createUniquePerSource = $state(false);
	let createAuditComment = $state('');
	let createError = $state<SchemaFormError>();

	let editTarget = $state<RelationshipTypeResponse>();
	let editSourceTypeId = $state('');
	let editTargetTypeId = $state('');
	let editUniquePerSource = $state(false);
	let editAuditComment = $state('');
	let editError = $state<SchemaFormError>();

	async function handleCreate(event: SubmitEvent) {
		event.preventDefault();
		submitting = true;
		createError = undefined;

		const { error } = await RelationshipTypeController.createRelationshipType({
			client: getApiClient(),
			body: {
				id: createId.trim(),
				sourceTypeId: createSourceTypeId,
				targetTypeId: createTargetTypeId,
				uniquePerSource: createUniquePerSource,
			},
			headers: auditHeaders(createAuditComment),
		});

		submitting = false;
		if (error) {
			createError = error;
			return;
		}

		toast.success(t('web.relationship_types.created'));
		createId = '';
		createSourceTypeId = '';
		createTargetTypeId = '';
		createUniquePerSource = false;
		createAuditComment = '';
		await refreshAll();
	}

	function openEdit(type: RelationshipTypeResponse) {
		if (isBuiltinResourceId(type.id)) return;
		editTarget = type;
		editSourceTypeId = type.sourceTypeId;
		editTargetTypeId = type.targetTypeId;
		editUniquePerSource = type.uniquePerSource;
		editAuditComment = '';
		editError = undefined;
	}

	async function handleEdit(event: SubmitEvent) {
		event.preventDefault();
		if (!editTarget) return;
		submitting = true;
		editError = undefined;

		const { error } = await RelationshipTypeController.updateRelationshipType({
			client: getApiClient(),
			path: { id: editTarget.id },
			body: {
				sourceTypeId: editSourceTypeId,
				targetTypeId: editTargetTypeId,
				uniquePerSource: editUniquePerSource,
			},
			headers: auditHeaders(editAuditComment),
		});

		submitting = false;
		if (error) {
			editError = error;
			return;
		}

		toast.success(t('web.relationship_types.updated'));
		editTarget = undefined;
		await refreshAll();
	}
</script>

<AdminPageIntro
	title={t('web.relationship_types.title')}
	intro={t('web.relationship_types.intro')}
	error={data.error}
/>

{#if !data.error}
	<TableCard
		title={t('web.relationship_types.table_title')}
		items={sortedItems}
		empty={t('web.relationship_types.empty')}
		getKey={type => type.id}
	>
		{#snippet header()}
			<th class="px-5 py-3 font-medium">{t('web.common.id')}</th>
			<th class="px-5 py-3 font-medium">{t('web.relationship_types.source_type')}</th>
			<th class="px-5 py-3 font-medium">{t('web.relationship_types.target_type')}</th>
			<th class="px-5 py-3 font-medium">{t('web.relationship_types.unique_per_source')}</th>
			<th class="px-5 py-3 font-medium"><span class="sr-only">{t('web.common.actions')}</span></th>
		{/snippet}
		{#snippet row(type)}
			<td class="px-5 py-4"><MonoId value={type.id} /></td>
			<td class="px-5 py-4">{type.sourceTypeId}</td>
			<td class="px-5 py-4">{type.targetTypeId}</td>
			<td class="px-5 py-4">{type.uniquePerSource ? t('web.common.yes') : t('web.common.no')}</td>
			<td class="px-5 py-4 text-right">
				<TypeEditButton id={type.id} disabled={submitting} onclick={() => openEdit(type)} />
			</td>
		{/snippet}
	</TableCard>

	<AdminCreateForm
		title={t('web.relationship_types.create_title')}
		hint={t('web.relationship_types.create_hint')}
		onsubmit={handleCreate}
		{submitting}
		submitDisabled={!createId.trim() || !createSourceTypeId || !createTargetTypeId}
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
				placeholder="custom:sponsors"
				required
				disabled={submitting}
			/>
		</label>
		<label class="mb-4 flex flex-col gap-1">
			<span class="text-label">{t('web.relationship_types.source_type')}</span>
			<select class="input w-full" name="sourceTypeId" bind:value={createSourceTypeId} required disabled={submitting}>
				<option value="" disabled>{t('web.relationship_types.source_type')}</option>
				{#each allHolderTypes as typeOption (typeOption.id)}
					<option value={typeOption.id}>{typeOption.id}</option>
				{/each}
			</select>
		</label>
		<label class="mb-4 flex flex-col gap-1">
			<span class="text-label">{t('web.relationship_types.target_type')}</span>
			<select class="input w-full" name="targetTypeId" bind:value={createTargetTypeId} required disabled={submitting}>
				<option value="" disabled>{t('web.relationship_types.target_type')}</option>
				{#each allHolderTypes as typeOption (typeOption.id)}
					<option value={typeOption.id}>{typeOption.id}</option>
				{/each}
			</select>
		</label>
		<label class="mb-4 flex items-center gap-2">
			<input type="checkbox" name="uniquePerSource" bind:checked={createUniquePerSource} disabled={submitting} />
			<span class="text-label">{t('web.relationship_types.unique_per_source')}</span>
		</label>
	</AdminCreateForm>

	<TargetDialog bind:target={editTarget} title="web.relationship_types.edit_title">
		{#snippet description(target)}
			{t('web.relationship_types.edit_description', target.id)}
		{/snippet}
		{#snippet body(_target)}
			<form id="relationship-type-edit-form" class="flex flex-col gap-4" onsubmit={handleEdit}>
				<label class="flex flex-col gap-1">
					<span class="text-label">{t('web.relationship_types.source_type')}</span>
					<select class="input w-full" name="sourceTypeId" bind:value={editSourceTypeId} required disabled={submitting}>
						{#each allHolderTypes as typeOption (typeOption.id)}
							<option value={typeOption.id}>{typeOption.id}</option>
						{/each}
					</select>
				</label>
				<label class="flex flex-col gap-1">
					<span class="text-label">{t('web.relationship_types.target_type')}</span>
					<select class="input w-full" name="targetTypeId" bind:value={editTargetTypeId} required disabled={submitting}>
						{#each allHolderTypes as typeOption (typeOption.id)}
							<option value={typeOption.id}>{typeOption.id}</option>
						{/each}
					</select>
				</label>
				<label class="flex items-center gap-2">
					<input type="checkbox" name="uniquePerSource" bind:checked={editUniquePerSource} disabled={submitting} />
					<span class="text-label">{t('web.relationship_types.unique_per_source')}</span>
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
				formId="relationship-type-edit-form"
				confirmLabel="web.common.save"
				confirmingLabel="web.common.saving"
				{submitting}
			/>
		{/snippet}
	</TargetDialog>
{/if}
