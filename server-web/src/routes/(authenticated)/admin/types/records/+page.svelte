<script lang="ts">
	import { RecordTypeController, SecurityFilterUsage, type RecordTypeResponse } from '#lib/api/index.js';
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
		formatContentTypes,
		isBuiltinResourceId,
		parseContentTypes,
	} from '#lib/types/typeAdmin.js';
	import { refreshAll } from '$app/navigation';
	import toast from 'svelte-hot-french-toast';

	let { data } = $props();

	const sortedTypes = $derived([...data.types].sort((a, b) => a.id.localeCompare(b.id)));
	const securityFilterUsages = Object.values(SecurityFilterUsage);

	let createId = $state('');
	let createContentTypes = $state('');
	let createSecurityFilter = $state('');
	let createSecurityFilterUsage = $state<SecurityFilterUsage>(SecurityFilterUsage.HIDE_FILES);
	let createSelectedIds = $state<string[]>([]);
	let createDefaults = $state<Record<string, string>>({});
	let createAuditComment = $state('');
	let createError = $state<SchemaFormError>();
	let submitting = $state(false);

	let editTarget = $state<RecordTypeResponse>();
	let editContentTypes = $state('');
	let editSecurityFilter = $state('');
	let editSecurityFilterUsage = $state<SecurityFilterUsage>(SecurityFilterUsage.HIDE_FILES);
	let editSelectedIds = $state<string[]>([]);
	let editDefaults = $state<Record<string, string>>({});
	let editAuditComment = $state('');
	let editError = $state<SchemaFormError>();

	async function handleCreate(event: SubmitEvent) {
		event.preventDefault();
		submitting = true;
		createError = undefined;

		const { error } = await RecordTypeController.createRecordType({
			client: getApiClient(),
			body: {
				id: createId.trim(),
				contentTypes: parseContentTypes(createContentTypes),
				securityFilter: createSecurityFilter.trim() || undefined,
				securityFilterUsage: createSecurityFilterUsage,
				properties: buildTypePropertyAssignments(createSelectedIds, createDefaults),
			},
			headers: auditHeaders(createAuditComment),
		});

		submitting = false;

		if (error) {
			createError = error;
			return;
		}

		toast.success(t('web.record_types.created'));
		createId = '';
		createContentTypes = '';
		createSecurityFilter = '';
		createSecurityFilterUsage = SecurityFilterUsage.HIDE_FILES;
		createSelectedIds = [];
		createDefaults = {};
		createAuditComment = '';
		await refreshAll();
	}

	function openEdit(type: RecordTypeResponse) {
		if (isBuiltinResourceId(type.id)) {
			return;
		}
		editTarget = type;
		editContentTypes = formatContentTypes(type.contentTypes);
		editSecurityFilter = type.securityFilter ?? '';
		editSecurityFilterUsage = type.securityFilterUsage;
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

		const { error } = await RecordTypeController.updateRecordType({
			client: getApiClient(),
			path: { id: editTarget.id },
			body: {
				contentTypes: parseContentTypes(editContentTypes),
				securityFilter: editSecurityFilter.trim() || undefined,
				securityFilterUsage: editSecurityFilterUsage,
				properties: buildTypePropertyAssignments(editSelectedIds, editDefaults),
			},
			headers: auditHeaders(editAuditComment),
		});

		submitting = false;

		if (error) {
			editError = error;
			return;
		}

		toast.success(t('web.record_types.updated'));
		editTarget = undefined;
		await refreshAll();
	}
</script>

<AdminPageIntro title={t('web.record_types.title')} intro={t('web.record_types.intro')} error={data.error} />

{#if !data.error}
	<TableCard
		title={t('web.record_types.table_title')}
		items={sortedTypes}
		empty={t('web.record_types.empty')}
		getKey={type => type.id}
	>
		{#snippet header()}
			<th class="px-5 py-3 font-medium">{t('web.common.id')}</th>
			<th class="px-5 py-3 font-medium">{t('web.record_types.security_usage')}</th>
			<th class="px-5 py-3 font-medium">{t('web.record_types.content_types')}</th>
			<th class="px-5 py-3 font-medium">{t('web.types.properties')}</th>
			<th class="px-5 py-3 font-medium"><span class="sr-only">{t('web.common.actions')}</span></th>
		{/snippet}
		{#snippet row(type)}
			<td class="px-5 py-4"><MonoId value={type.id} /></td>
			<td class="px-5 py-4">{type.securityFilterUsage}</td>
			<td class="px-5 py-4">{formatContentTypes(type.contentTypes) || t('web.common.none')}</td>
			<td class="px-5 py-4">{type.properties.length}</td>
			<td class="px-5 py-4 text-right">
				<TypeEditButton id={type.id} disabled={submitting} onclick={() => openEdit(type)} />
			</td>
		{/snippet}
	</TableCard>

	<AdminCreateForm
		title={t('web.record_types.create_title')}
		hint={t('web.record_types.create_hint')}
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
				placeholder="custom:my_type"
				required
				disabled={submitting}
			/>
		</label>

		<label class="mb-4 flex flex-col gap-1">
			<span class="text-label">{t('web.record_types.content_types')}</span>
			<input
				class="input w-full"
				name="contentTypes"
				bind:value={createContentTypes}
				placeholder="text/plain, application/pdf"
				disabled={submitting}
			/>
		</label>

		<label class="mb-4 flex flex-col gap-1">
			<span class="text-label">{t('web.record_types.security_filter')}</span>
			<textarea
				class="input w-full font-mono"
				name="securityFilter"
				bind:value={createSecurityFilter}
				rows={2}
				disabled={submitting}></textarea>
		</label>

		<label class="mb-4 flex flex-col gap-1">
			<span class="text-label">{t('web.record_types.security_usage')}</span>
			<select
				class="input w-full"
				name="securityFilterUsage"
				bind:value={createSecurityFilterUsage}
				disabled={submitting}
			>
				{#each securityFilterUsages as usage (usage)}
					<option value={usage}>{usage}</option>
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

<TargetDialog bind:target={editTarget} title="web.record_types.edit_title" size="wide">
	{#snippet description(target)}
		{t('web.record_types.edit_description', target.id)}
	{/snippet}
	{#snippet body(_target)}
		<form id="record-type-edit-form" class="flex flex-col gap-4" onsubmit={handleEdit}>
			<label class="flex flex-col gap-1">
				<span class="text-label">{t('web.record_types.content_types')}</span>
				<input class="input w-full" name="contentTypes" bind:value={editContentTypes} disabled={submitting} />
			</label>

			<label class="flex flex-col gap-1">
				<span class="text-label">{t('web.record_types.security_filter')}</span>
				<textarea
					class="input w-full font-mono"
					name="securityFilter"
					bind:value={editSecurityFilter}
					rows={2}
					disabled={submitting}></textarea>
			</label>

			<label class="flex flex-col gap-1">
				<span class="text-label">{t('web.record_types.security_usage')}</span>
				<select
					class="input w-full"
					name="securityFilterUsage"
					bind:value={editSecurityFilterUsage}
					disabled={submitting}
				>
					{#each securityFilterUsages as usage (usage)}
						<option value={usage}>{usage}</option>
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
			formId="record-type-edit-form"
			confirmLabel="web.common.save"
			confirmingLabel="web.common.saving"
			{submitting}
		/>
	{/snippet}
</TargetDialog>
