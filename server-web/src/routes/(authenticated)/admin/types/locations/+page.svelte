<script lang="ts">
	import { LocationKind, LocationTypeController, type LocationTypeResponse } from '#lib/api/index.js';
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
	import TypePropertyAssignmentsEditor from '#lib/components/TypePropertyAssignmentsEditor.svelte';
	import type { SchemaFormError } from '#lib/components/SchemaForm.svelte';
	import { t } from '#lib/i18n/catalog.js';
	import {
		assignmentsFromTypeProperties,
		buildTypePropertyAssignments,
		isBuiltinResourceId,
		LOCATION_TYPE_KINDS,
	} from '#lib/types/typeAdmin.js';
	import { refreshAll } from '$app/navigation';
	import toast from 'svelte-hot-french-toast';

	let { data } = $props();

	const sortedTypes = $derived([...data.types].sort((a, b) => a.id.localeCompare(b.id)));

	let createId = $state('');
	let createKind = $state<LocationKind>(LocationKind.USER);
	let createSelectedIds = $state<string[]>([]);
	let createDefaults = $state<Record<string, string>>({});
	let createAuditComment = $state('');
	let createError = $state<SchemaFormError>();
	let submitting = $state(false);

	let editTarget = $state<LocationTypeResponse>();
	let editKind = $state<LocationKind>(LocationKind.USER);
	let editSelectedIds = $state<string[]>([]);
	let editDefaults = $state<Record<string, string>>({});
	let editAuditComment = $state('');
	let editError = $state<SchemaFormError>();

	async function handleCreate(event: SubmitEvent) {
		event.preventDefault();
		submitting = true;
		createError = undefined;

		const { error } = await LocationTypeController.createLocationType({
			client: getApiClient(),
			body: {
				id: createId.trim(),
				kind: createKind,
				properties: buildTypePropertyAssignments(createSelectedIds, createDefaults),
			},
			headers: auditHeaders(createAuditComment),
		});

		submitting = false;

		if (error) {
			createError = error;
			return;
		}

		toast.success(t('web.location_types.created'));
		createId = '';
		createKind = LocationKind.USER;
		createSelectedIds = [];
		createDefaults = {};
		createAuditComment = '';
		await refreshAll();
	}

	function openEdit(type: LocationTypeResponse) {
		if (isBuiltinResourceId(type.id)) {
			return;
		}
		editTarget = type;
		editKind = type.kind === LocationKind.GROUP ? LocationKind.GROUP : LocationKind.USER;
		const assignments = assignmentsFromTypeProperties(type.properties);
		editSelectedIds = assignments.selectedIds;
		editDefaults = assignments.defaults;
		editAuditComment = '';
		editError = undefined;
	}

	async function handleEdit(event: SubmitEvent) {
		event.preventDefault();
		if (!editTarget) return;

		submitting = true;
		editError = undefined;

		const { error } = await LocationTypeController.updateLocationType({
			client: getApiClient(),
			path: { id: editTarget.id },
			body: {
				kind: editKind,
				properties: buildTypePropertyAssignments(editSelectedIds, editDefaults),
			},
			headers: auditHeaders(editAuditComment),
		});

		submitting = false;

		if (error) {
			editError = error;
			return;
		}

		toast.success(t('web.location_types.updated'));
		editTarget = undefined;
		await refreshAll();
	}
</script>

<AdminPageIntro title={t('web.location_types.title')} intro={t('web.location_types.intro')} error={data.error} />

{#if !data.error}
	<TableCard
		title={t('web.location_types.table_title')}
		items={sortedTypes}
		empty={t('web.location_types.empty')}
		getKey={type => type.id}
	>
		{#snippet header()}
			<th class="px-5 py-3 font-medium">{t('web.common.id')}</th>
			<th class="px-5 py-3 font-medium">{t('web.location_types.kind')}</th>
			<th class="px-5 py-3 font-medium">{t('web.types.properties')}</th>
			<th class="px-5 py-3 font-medium"><span class="sr-only">{t('web.common.actions')}</span></th>
		{/snippet}
		{#snippet row(type)}
			<td class="px-5 py-4"><MonoId value={type.id} /></td>
			<td class="px-5 py-4">{type.kind}</td>
			<td class="px-5 py-4">{type.properties.length}</td>
			<td class="px-5 py-4 text-right">
				<TypeEditButton id={type.id} disabled={submitting} onclick={() => openEdit(type)} />
			</td>
		{/snippet}
	</TableCard>

	<AdminCreateForm
		title={t('web.location_types.create_title')}
		hint={t('web.location_types.create_hint')}
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
				placeholder="custom:my_location_type"
				required
				disabled={submitting}
			/>
		</label>

		<label class="mb-4 flex flex-col gap-1">
			<span class="text-label">{t('web.location_types.kind')}</span>
			<select class="input w-full" name="kind" bind:value={createKind} disabled={submitting}>
				{#each LOCATION_TYPE_KINDS as kind (kind)}
					<option value={kind}>{kind}</option>
				{/each}
			</select>
		</label>

		<div class="mb-4">
			<TypePropertyAssignmentsEditor
				properties={data.properties}
				bind:selectedIds={createSelectedIds}
				bind:defaults={createDefaults}
				disabled={submitting}
			/>
		</div>
	</AdminCreateForm>
{/if}

<TargetDialog bind:target={editTarget} title="web.location_types.edit_title" size="wide">
	{#snippet description(target)}
		{t('web.location_types.edit_description', target.id)}
	{/snippet}
	{#snippet body(_target)}
		<form id="location-type-edit-form" class="flex flex-col gap-4" onsubmit={handleEdit}>
			<label class="flex flex-col gap-1">
				<span class="text-label">{t('web.location_types.kind')}</span>
				<select class="input w-full" name="kind" bind:value={editKind} disabled={submitting}>
					{#each LOCATION_TYPE_KINDS as kind (kind)}
						<option value={kind}>{kind}</option>
					{/each}
				</select>
			</label>

			<TypePropertyAssignmentsEditor
				properties={data.properties}
				bind:selectedIds={editSelectedIds}
				bind:defaults={editDefaults}
				disabled={submitting}
			/>

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
			formId="location-type-edit-form"
			confirmLabel="web.common.save"
			confirmingLabel="web.common.saving"
			{submitting}
		/>
	{/snippet}
</TargetDialog>
