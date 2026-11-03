<script lang="ts">
	import { ManagedObjectKind, LocationTypeController, type LocationTypeResponse } from '#lib/api/index.js';
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
		asLocationTypeKind,
		assignmentsFromTypeProperties,
		buildTypePropertyAssignments,
		inheritedPropertiesFromBase,
		isBuiltinResourceId,
		LOCATION_TYPE_KINDS,
		type LocationTypeKind,
		withoutInheritedPropertyIds,
	} from '#lib/types/typeAdmin.js';
	import { refreshAll } from '$app/navigation';
	import toast from 'svelte-hot-french-toast';

	let { data } = $props();

	let createId = $state('');
	let createKind = $state<LocationTypeKind | undefined>(ManagedObjectKind.USER);
	let createAbstract = $state(false);
	let createBaseTypeId = $state('');
	let createSelectedIds = $state<string[]>([]);
	let createDefaults = $state<Record<string, string>>({});
	let createAuditComment = $state('');
	let createError = $state<SchemaFormError>();
	let submitting = $state(false);

	let editTarget = $state<LocationTypeResponse>();
	let editKind = $state<LocationTypeKind | undefined>(ManagedObjectKind.USER);
	let editAbstract = $state(false);
	let editBaseTypeId = $state('');
	let editSelectedIds = $state<string[]>([]);
	let editDefaults = $state<Record<string, string>>({});
	let editAuditComment = $state('');
	let editError = $state<SchemaFormError>();

	const sortedTypes = $derived([...data.types].sort((a, b) => a.id.localeCompare(b.id)));
	const createKindOptions = $derived(
		createAbstract ? [...LOCATION_TYPE_KINDS, undefined] : [...LOCATION_TYPE_KINDS]
	);
	const editKindOptions = $derived(
		editAbstract ? [...LOCATION_TYPE_KINDS, undefined] : [...LOCATION_TYPE_KINDS]
	);
	const createInheritedProperties = $derived(
		inheritedPropertiesFromBase(sortedTypes, createBaseTypeId)
	);
	const editInheritedProperties = $derived(inheritedPropertiesFromBase(sortedTypes, editBaseTypeId));

	function onCreateBaseTypeChange() {
		createSelectedIds = withoutInheritedPropertyIds(
			createSelectedIds,
			inheritedPropertiesFromBase(sortedTypes, createBaseTypeId)
		);
	}

	function onEditBaseTypeChange() {
		editSelectedIds = withoutInheritedPropertyIds(
			editSelectedIds,
			inheritedPropertiesFromBase(sortedTypes, editBaseTypeId)
		);
	}

	async function handleCreate(event: SubmitEvent) {
		event.preventDefault();
		submitting = true;
		createError = undefined;

		const { error } = await LocationTypeController.createLocationType({
			client: getApiClient(),
			body: {
				id: createId.trim(),
				kind: createKind,
				abstractType: createAbstract,
				baseTypeId: createBaseTypeId || undefined,
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
		createKind = ManagedObjectKind.USER;
		createAbstract = false;
		createBaseTypeId = '';
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
		editKind = asLocationTypeKind(type.kind);
		editAbstract = type.abstractType;
		editBaseTypeId = type.baseTypeId ?? '';
		const assignments = assignmentsFromTypeProperties(type.declaredProperties);
		const inherited = inheritedPropertiesFromBase(sortedTypes, type.baseTypeId ?? '');
		editSelectedIds = withoutInheritedPropertyIds(assignments.selectedIds, inherited);
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
				abstractType: editAbstract,
				baseTypeId: editBaseTypeId || undefined,
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
			<th class="px-5 py-3 font-medium">{t('web.location_types.abstract')}</th>
			<th class="px-5 py-3 font-medium">{t('web.location_types.base_type')}</th>
			<th class="px-5 py-3 font-medium">{t('web.types.properties')}</th>
			<th class="px-5 py-3 font-medium"><span class="sr-only">{t('web.common.actions')}</span></th>
		{/snippet}
		{#snippet row(type)}
			<td class="px-5 py-4"><MonoId value={type.id} /></td>
			<td class="px-5 py-4">{type.kind ?? t('web.location_types.kind_any')}</td>
			<td class="px-5 py-4">{type.abstractType ? t('web.common.yes') : t('web.common.no')}</td>
			<td class="px-5 py-4">{type.baseTypeId ?? t('web.location_types.base_type_none')}</td>
			<td class="px-5 py-4">{type.effectiveProperties.length}</td>
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
			<select
				class="input w-full"
				name="kind"
				value={createKind ?? ''}
				onchange={event => {
					createKind = asLocationTypeKind(event.currentTarget.value);
				}}
				disabled={submitting}
			>
				{#each createKindOptions as kind (kind ?? 'any')}
					<option value={kind ?? ''}>
						{kind == null ? t('web.location_types.kind_any') : kind}
					</option>
				{/each}
			</select>
		</label>

		<label class="mb-4 flex items-center gap-2">
			<input
				type="checkbox"
				name="abstractType"
				bind:checked={createAbstract}
				disabled={submitting}
			/>
			<span class="text-label">{t('web.location_types.abstract')}</span>
		</label>

		<label class="mb-4 flex flex-col gap-1">
			<span class="text-label">{t('web.location_types.base_type')}</span>
			<select
				class="input w-full"
				name="baseTypeId"
				bind:value={createBaseTypeId}
				onchange={onCreateBaseTypeChange}
				disabled={submitting}
			>
				<option value="">{t('web.location_types.base_type_none')}</option>
				{#each sortedTypes as typeOption (typeOption.id)}
					<option value={typeOption.id}>{typeOption.id}</option>
				{/each}
			</select>
		</label>

		<div class="mb-4">
			<TypePropertyAssignmentsEditor
				properties={data.properties}
				bind:selectedIds={createSelectedIds}
				bind:defaults={createDefaults}
				inheritedProperties={createInheritedProperties}
				disabled={submitting}
			/>
		</div>
	</AdminCreateForm>
{/if}

<TargetDialog bind:target={editTarget} title="web.location_types.edit_title" size="wide">
	{#snippet description(target)}
		{t('web.location_types.edit_description', target.id)}
	{/snippet}
	{#snippet body(target)}
		<form id="location-type-edit-form" class="flex flex-col gap-4" onsubmit={handleEdit}>
			<label class="flex flex-col gap-1">
				<span class="text-label">{t('web.location_types.kind')}</span>
				<select
					class="input w-full"
					name="kind"
					value={editKind ?? ''}
					onchange={event => {
						editKind = asLocationTypeKind(event.currentTarget.value);
					}}
					disabled={submitting}
				>
					{#each editKindOptions as kind (kind ?? 'any')}
						<option value={kind ?? ''}>
							{kind == null ? t('web.location_types.kind_any') : kind}
						</option>
					{/each}
				</select>
			</label>

			<label class="flex items-center gap-2">
				<input
					type="checkbox"
					name="abstractType"
					bind:checked={editAbstract}
					disabled={submitting}
				/>
				<span class="text-label">{t('web.location_types.abstract')}</span>
			</label>

			<label class="flex flex-col gap-1">
				<span class="text-label">{t('web.location_types.base_type')}</span>
				<select
					class="input w-full"
					name="baseTypeId"
					bind:value={editBaseTypeId}
					onchange={onEditBaseTypeChange}
					disabled={submitting}
				>
					<option value="">{t('web.location_types.base_type_none')}</option>
					{#each sortedTypes as typeOption (typeOption.id)}
						{#if typeOption.id !== target.id}
							<option value={typeOption.id}>{typeOption.id}</option>
						{/if}
					{/each}
				</select>
			</label>

			<TypePropertyAssignmentsEditor
				properties={data.properties}
				bind:selectedIds={editSelectedIds}
				bind:defaults={editDefaults}
				inheritedProperties={editInheritedProperties}
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
