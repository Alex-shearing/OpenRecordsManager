<script lang="ts">
	import { FileStoreController } from '#lib/api/index.js';
	import type { FileStoreResponse, SimpleFileStoreResponse } from '#lib/api/types.gen.js';
	import { auditHeaders, getApiClient } from '#lib/api-client.js';
	import AdminFormError from '#lib/components/AdminFormError.svelte';
	import AdminPageIntro from '#lib/components/AdminPageIntro.svelte';
	import AuditCommentField from '#lib/components/AuditCommentField.svelte';
	import DialogActions from '#lib/components/DialogActions.svelte';
	import MiddlewarePicker from '#lib/components/MiddlewarePicker.svelte';
	import MonoId from '#lib/components/MonoId.svelte';
	import SchemaForm from '#lib/components/SchemaForm.svelte';
	import { type SchemaFormError } from '#lib/components/SchemaForm.svelte';
	import TableCard from '#lib/components/TableCard.svelte';
	import TargetDialog from '#lib/components/TargetDialog.svelte';
	import { t, tApiErrorResponse } from '#lib/i18n/catalog.js';
	import { invalidateAll } from '$app/navigation';
	import toast from 'svelte-hot-french-toast';

	let { data } = $props();

	const sortedStores = $derived(
		[...data.stores].sort((a, b) => {
			const nameCmp = (a.name ?? '').localeCompare(b.name ?? '');
			return nameCmp !== 0 ? nameCmp : (a.id ?? '').localeCompare(b.id ?? '');
		})
	);
	const sortedTypes = $derived([...data.types].sort((a, b) => a.id.localeCompare(b.id)));

	// svelte-ignore state_referenced_locally
	let createTypeId = $state(data.types.at(0)?.id ?? '');
	let createName = $state('');
	let createValues = $state<Record<string, string>>({});
	let selectedMiddlewareIds = $state<string[]>([]);
	let createError = $state<SchemaFormError>();
	let createAuditComment = $state('');
	let submitting = $state(false);

	const createType = $derived(sortedTypes.find(type => type.id === createTypeId));

	let editTarget = $state<FileStoreResponse>();
	let editName = $state('');
	let editValues = $state<Record<string, string>>({});
	let editError = $state<SchemaFormError>();
	let editAuditComment = $state('');
	let editLoading = $state(false);

	let deleteTarget = $state<SimpleFileStoreResponse>();
	let deleteError = $state<SchemaFormError>();
	let deleteAuditComment = $state('');

	function toFormValues(properties: Record<string, unknown> | undefined): Record<string, string> {
		return Object.fromEntries(
			Object.entries(properties ?? {}).map(([key, value]) => [key, value == null ? '' : String(value)])
		);
	}

	async function handleCreate(event: SubmitEvent) {
		event.preventDefault();
		if (!createTypeId) {
			createError = { error: 'web.file_stores.select_type' };
			return;
		}

		submitting = true;
		createError = undefined;

		const { error } = await FileStoreController.fileStoreCreate({
			client: getApiClient(),
			body: {
				name: createName,
				type: createTypeId,
				properties: createValues,
				middlewares: selectedMiddlewareIds,
			},
			headers: auditHeaders(createAuditComment),
		});

		submitting = false;

		if (error) {
			createError = error;
			return;
		}

		toast.success(t('web.file_stores.created'));

		createName = '';
		createValues = {};
		selectedMiddlewareIds = [];
		createError = undefined;
		createAuditComment = '';

		await invalidateAll();
	}

	async function openEdit(store: SimpleFileStoreResponse) {
		editLoading = true;
		editValues = {};
		editError = undefined;
		editAuditComment = '';

		const { data: result, error } = await FileStoreController.fileStoreRetrieveOne({
			client: getApiClient(),
			path: { id: store.id },
		});

		editLoading = false;

		if (error) {
			toast.error(tApiErrorResponse(error));
			return;
		}

		editTarget = result.data;
		editName = result.data.name;
		editValues = toFormValues(result.data.properties);
	}

	async function handleEdit(event: SubmitEvent) {
		event.preventDefault();
		if (!editTarget) return;

		submitting = true;
		editError = undefined;

		const { error } = await FileStoreController.fileStoreUpdate({
			client: getApiClient(),
			path: { id: editTarget.id },
			body: {
				name: editName,
				properties: editValues,
			},
			headers: auditHeaders(editAuditComment),
		});

		submitting = false;

		if (error) {
			editError = error;
			return;
		}

		toast.success(t('web.file_stores.updated'));
		editTarget = undefined;
		await invalidateAll();
	}

	async function handleDelete(event: SubmitEvent) {
		event.preventDefault();
		if (!deleteTarget) return;

		submitting = true;
		deleteError = undefined;

		const { error } = await FileStoreController.fileStoreDelete({
			client: getApiClient(),
			path: { id: deleteTarget.id },
			headers: auditHeaders(deleteAuditComment),
		});

		submitting = false;

		if (error) {
			deleteError = error;
			return;
		}

		toast.success(t('web.file_stores.deleted'));
		deleteTarget = undefined;
		await invalidateAll();
	}
</script>

<AdminPageIntro title={t('web.file_stores.title')} intro={t('web.file_stores.intro')} error={data.error} />

{#if !data.error}
	<TableCard
		title={t('web.file_stores.table_title')}
		items={sortedStores}
		empty={t('web.file_stores.empty')}
		getKey={s => s.id}
	>
		{#snippet header()}
			<th class="px-5 py-3 font-medium">{t('web.common.name')}</th>
			<th class="px-5 py-3 font-medium">{t('web.common.type')}</th>
			<th class="px-5 py-3 font-medium">{t('web.common.id')}</th>
			<th class="px-5 py-3 font-medium"><span class="sr-only">{t('web.common.actions')}</span></th>
		{/snippet}
		{#snippet row(store)}
			<td class="px-5 py-4 font-medium">{store.name}</td>
			<td class="px-5 py-4">{store.type}</td>
			<td class="px-5 py-4">
				<MonoId value={store.id} />
			</td>
			<td class="px-5 py-4 text-right">
				<button type="button" class="btn-ghost" disabled={submitting || editLoading} onclick={() => openEdit(store)}>
					{t('web.common.edit')}
				</button>
				<button
					type="button"
					class="btn-ghost text-destructive"
					disabled={submitting || editLoading}
					onclick={() => {
						deleteTarget = store;
						deleteAuditComment = '';
						deleteError = undefined;
					}}
				>
					{t('web.common.delete')}
				</button>
			</td>
		{/snippet}
	</TableCard>

	<form class="mt-6 card p-5" onsubmit={handleCreate}>
		<h2 class="mb-1 text-lg font-medium">{t('web.file_stores.create_title')}</h2>
		<p class="mb-4 text-hint">{t('web.file_stores.create_hint')}</p>

		{#if sortedTypes.length === 0}
			<p class="text-hint">{t('web.file_stores.no_types')}</p>
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
					{#each sortedTypes as type (type.id)}
						<option value={type.id}>{type.id}</option>
					{/each}
				</select>
			</label>

			{#if createType}
				{#key createTypeId}
					<SchemaForm
						schema={createType.settingsSchema}
						bind:values={createValues}
						error={createError}
						{submitting}
						idPrefix="file-store-create"
					>
						{#snippet after()}
							<MiddlewarePicker
								middlewares={data.middlewares}
								bind:selected={selectedMiddlewareIds}
								disabled={submitting}
							/>

							<AuditCommentField
								bind:value={createAuditComment}
								required={data.auditCommentRequired.create}
								disabled={submitting}
							/>
						{/snippet}
					</SchemaForm>
				{/key}
			{/if}

			<div class="mt-4">
				<button type="submit" class="btn-primary" disabled={submitting || !createTypeId || !createName}>
					{submitting ? t('web.common.creating') : t('web.common.create')}
				</button>
			</div>
		{/if}
	</form>
{/if}

<TargetDialog bind:target={editTarget} title="web.file_stores.edit_title" size="wide">
	{#snippet description(target)}
		{t('web.file_stores.edit_description', target.id)}
	{/snippet}
	{#snippet body(target)}
		{@const targetType = sortedTypes.find(type => type.id === editTarget?.type)}
		<form id="file-store-edit-form" class="flex flex-col gap-4" onsubmit={handleEdit}>
			<span class="flex flex-col gap-1">{t('web.file_stores.files_in_store', target.fileCount)}</span>

			<label class="flex flex-col gap-1">
				<span class="text-label">{t('web.common.name')}</span>
				<input class="input w-full" bind:value={editName} required disabled={submitting} />
			</label>

			<label class="flex flex-col gap-1">
				<span class="text-label">{t('web.common.type')}</span>
				<input class="input w-full" value={target.type} readonly disabled />
			</label>

			{#if targetType}
				{#key target.id}
					<SchemaForm
						schema={targetType.settingsSchema}
						bind:values={editValues}
						error={editError}
						{submitting}
						idPrefix="file-store-edit"
					></SchemaForm>
				{/key}
			{:else}
				<p class="text-sm text-destructive" role="alert">{t('web.file_stores.unknown_type')}</p>
			{/if}

			<div class="flex flex-col gap-1">
				<span class="text-label">{t('web.file_stores.middlewares')}</span>
				{#if target.middlewares.length === 0}
					<p class="text-hint">{t('web.common.none')}</p>
				{:else}
					<ul class="flex flex-col gap-2">
						{#each target.middlewares as id (id)}
							{@const middleware = data.middlewares.find(m => m.id === id)}
							<li>
								<span class="font-medium">{middleware?.name ?? t('web.common.unknown')}</span>
								<span class="block"><MonoId value={id} muted /></span>
							</li>
						{/each}
					</ul>
				{/if}
			</div>

			<AuditCommentField
				bind:value={editAuditComment}
				required={data.auditCommentRequired.update}
				disabled={submitting}
			/>
		</form>
	{/snippet}
	{#snippet footer()}
		<DialogActions
			formId="file-store-edit-form"
			confirmLabel="web.common.save"
			confirmingLabel="web.common.saving"
			{submitting}
		/>
	{/snippet}
</TargetDialog>

<TargetDialog
	bind:target={deleteTarget}
	title="web.file_stores.delete_title"
	onclose={() => {
		deleteError = undefined;
		deleteAuditComment = '';
	}}
>
	{#snippet description(target)}
		{t('web.file_stores.delete_confirm', target.name ?? '', target.id)}
	{/snippet}
	{#snippet body()}
		<form id="file-store-delete-form" class="flex flex-col gap-4" onsubmit={handleDelete}>
			<AuditCommentField
				bind:value={deleteAuditComment}
				required={data.auditCommentRequired.delete}
				disabled={submitting}
			/>
			<AdminFormError error={deleteError} />
		</form>
	{/snippet}
	{#snippet footer()}
		<DialogActions
			formId="file-store-delete-form"
			variant="destructive"
			confirmLabel="web.common.delete"
			confirmingLabel="web.common.deleting"
			{submitting}
		/>
	{/snippet}
</TargetDialog>
