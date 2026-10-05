<script lang="ts">
	import {
		LocationKind,
		LocationRelationshipTypeController,
		type LocationRelationshipTypeResponse,
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
	import { t } from '#lib/i18n/catalog.js';
	import { isBuiltinResourceId } from '#lib/types/typeAdmin.js';
	import { invalidateAll, refreshAll } from '$app/navigation';
	import toast from 'svelte-hot-french-toast';

	let { data } = $props();

	const sorted = $derived([...data.types].sort((a, b) => a.id.localeCompare(b.id)));
	const locationKinds = Object.values(LocationKind);

	let createId = $state('');
	let createSourceKind = $state<LocationKind>(LocationKind.USER);
	let createTargetKind = $state<LocationKind>(LocationKind.GROUP);
	let createUniquePerSource = $state(false);
	let createAuditComment = $state('');
	let createError = $state<SchemaFormError>();
	let submitting = $state(false);

	let editTarget = $state<LocationRelationshipTypeResponse>();
	let editSourceKind = $state<LocationKind>(LocationKind.USER);
	let editTargetKind = $state<LocationKind>(LocationKind.GROUP);
	let editUniquePerSource = $state(false);
	let editAuditComment = $state('');
	let editError = $state<SchemaFormError>();

	async function handleCreate(event: SubmitEvent) {
		event.preventDefault();
		submitting = true;
		createError = undefined;

		const { error } = await LocationRelationshipTypeController.createLocationRelationshipType({
			client: getApiClient(),
			body: {
				id: createId.trim(),
				sourceKind: createSourceKind,
				targetKind: createTargetKind,
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
		createSourceKind = LocationKind.USER;
		createTargetKind = LocationKind.GROUP;
		createUniquePerSource = false;
		createAuditComment = '';
		await invalidateAll();
	}

	function openEdit(type: LocationRelationshipTypeResponse) {
		if (isBuiltinResourceId(type.id)) return;
		editTarget = type;
		editSourceKind = type.sourceKind;
		editTargetKind = type.targetKind;
		editUniquePerSource = type.uniquePerSource;
		editAuditComment = '';
		editError = undefined;
	}

	async function handleEdit(event: SubmitEvent) {
		event.preventDefault();
		if (!editTarget) return;
		submitting = true;
		editError = undefined;

		const { error } = await LocationRelationshipTypeController.updateLocationRelationshipType({
			client: getApiClient(),
			path: { id: editTarget.id },
			body: {
				sourceKind: editSourceKind,
				targetKind: editTargetKind,
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
		items={sorted}
		empty={t('web.relationship_types.empty')}
		getKey={type => type.id}
	>
		{#snippet header()}
			<th class="px-5 py-3 font-medium">{t('web.common.id')}</th>
			<th class="px-5 py-3 font-medium">{t('web.relationship_types.source_kind')}</th>
			<th class="px-5 py-3 font-medium">{t('web.relationship_types.target_kind')}</th>
			<th class="px-5 py-3 font-medium">{t('web.relationship_types.unique_per_source')}</th>
			<th class="px-5 py-3 font-medium"><span class="sr-only">{t('web.common.actions')}</span></th>
		{/snippet}
		{#snippet row(type)}
			<td class="px-5 py-4"><MonoId value={type.id} /></td>
			<td class="px-5 py-4">{type.sourceKind}</td>
			<td class="px-5 py-4">{type.targetKind}</td>
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
		submitDisabled={!createId.trim()}
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
			<span class="text-label">{t('web.relationship_types.source_kind')}</span>
			<select class="input w-full" name="sourceKind" bind:value={createSourceKind} disabled={submitting}>
				{#each locationKinds as kind (kind)}
					<option value={kind}>{kind}</option>
				{/each}
			</select>
		</label>
		<label class="mb-4 flex flex-col gap-1">
			<span class="text-label">{t('web.relationship_types.target_kind')}</span>
			<select class="input w-full" name="targetKind" bind:value={createTargetKind} disabled={submitting}>
				{#each locationKinds as kind (kind)}
					<option value={kind}>{kind}</option>
				{/each}
			</select>
		</label>
		<label class="mb-4 flex items-center gap-2">
			<input type="checkbox" name="uniquePerSource" bind:checked={createUniquePerSource} disabled={submitting} />
			<span class="text-label">{t('web.relationship_types.unique_per_source')}</span>
		</label>
	</AdminCreateForm>
{/if}

<TargetDialog bind:target={editTarget} title="web.relationship_types.edit_title">
	{#snippet description(target)}
		{t('web.relationship_types.edit_description', target.id)}
	{/snippet}
	{#snippet body(_target)}
		<form id="relationship-type-edit-form" class="flex flex-col gap-4" onsubmit={handleEdit}>
			<label class="flex flex-col gap-1">
				<span class="text-label">{t('web.relationship_types.source_kind')}</span>
				<select class="input w-full" name="sourceKind" bind:value={editSourceKind} disabled={submitting}>
					{#each locationKinds as kind (kind)}
						<option value={kind}>{kind}</option>
					{/each}
				</select>
			</label>
			<label class="flex flex-col gap-1">
				<span class="text-label">{t('web.relationship_types.target_kind')}</span>
				<select class="input w-full" name="targetKind" bind:value={editTargetKind} disabled={submitting}>
					{#each locationKinds as kind (kind)}
						<option value={kind}>{kind}</option>
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
