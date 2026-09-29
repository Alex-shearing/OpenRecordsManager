<script lang="ts">
	import { FileStoreController } from '$lib/api';
	import type { MiddlewareResponse, SimpleMiddlewareResponse } from '$lib/api/types.gen';
	import { auditHeaders, getApiClient } from '$lib/api-client';
	import DialogActions from '$lib/components/DialogActions.svelte';
	import MonoId from '$lib/components/MonoId.svelte';
	import SchemaForm from '$lib/components/SchemaForm.svelte';
	import TableCard from '$lib/components/TableCard.svelte';
	import TargetDialog from '$lib/components/TargetDialog.svelte';
	import { t } from '$lib/i18n/catalog';
	import { invalidateAll } from '$app/navigation';
	import toast from 'svelte-hot-french-toast';

	let { data } = $props();

	const sortedMiddlewares = $derived(
		[...data.middlewares].sort((a, b) => {
			const nameCmp = (a.name ?? '').localeCompare(b.name ?? '');
			return nameCmp !== 0 ? nameCmp : (a.id ?? '').localeCompare(b.id ?? '');
		})
	);
	const sortedTypes = $derived([...data.types].sort((a, b) => a.id.localeCompare(b.id)));

	// svelte-ignore state_referenced_locally
	let createTypeId = $state(data.types.at(0)?.id ?? '');
	let createName = $state('');
	let createValues = $state<Record<string, string>>({});
	let createFieldErrors = $state<Record<string, string>>({});
	let createFormError = $state('');
	let createAuditComment = $state('');
	let submitting = $state(false);

	let editTarget = $state<MiddlewareResponse>();
	let editName = $state('');
	let editValues = $state<Record<string, string>>({});
	let editFieldErrors = $state<Record<string, string>>({});
	let editFormError = $state('');
	let editAuditComment = $state('');
	let editLoading = $state(false);

	let deleteTarget = $state<SimpleMiddlewareResponse>();
	let deleteFormError = $state('');
	let deleteAuditComment = $state('');

	const createType = $derived(sortedTypes.find(type => type.id === createTypeId));

	function toFormValues(properties: Record<string, unknown> | undefined): Record<string, string> {
		return Object.fromEntries(
			Object.entries(properties ?? {}).map(([key, value]) => [key, value == null ? '' : String(value)])
		);
	}

	function resetCreateForm() {
		createName = '';
		createValues = {};
		createFieldErrors = {};
		createFormError = '';
		createAuditComment = '';
	}

	async function handleCreate(event: SubmitEvent) {
		event.preventDefault();
		if (!createTypeId) {
			createFormError = t('web.middlewares.select_type');
			return;
		}

		submitting = true;
		createFieldErrors = {};
		createFormError = '';

		const { error } = await FileStoreController.middlewareCreate({
			client: getApiClient(),
			body: {
				name: createName,
				type: createTypeId,
				properties: createValues,
			},
			headers: auditHeaders(createAuditComment),
		});

		submitting = false;

		if (error) {
			createFieldErrors = (error.errorData ?? {}) as Record<string, string>;
			createFormError = error.error ?? t('web.middlewares.create_failed');
			return;
		}

		toast.success(t('web.middlewares.created'));
		resetCreateForm();
		await invalidateAll();
	}

	async function openEdit(middleware: SimpleMiddlewareResponse) {
		editLoading = true;
		editValues = {};
		editFieldErrors = {};
		editFormError = '';
		editAuditComment = '';

		const { data: result, error } = await FileStoreController.middlewareRetrieveOne({
			client: getApiClient(),
			path: { id: middleware.id },
		});

		editLoading = false;

		if (error) {
			toast.error(error.error ?? t('web.middlewares.load_failed', middleware.id));
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
		editFieldErrors = {};
		editFormError = '';

		const { error } = await FileStoreController.middlewareUpdate({
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
			editFieldErrors = (error.errorData ?? {}) as Record<string, string>;
			editFormError = error.error ?? t('web.middlewares.update_failed');
			return;
		}

		toast.success(t('web.middlewares.updated'));
		editTarget = undefined;
		await invalidateAll();
	}

	async function handleDelete(event: SubmitEvent) {
		event.preventDefault();
		if (!deleteTarget) return;

		submitting = true;
		deleteFormError = '';

		const { error, response } = await FileStoreController.middlewareDelete({
			client: getApiClient(),
			path: { id: deleteTarget.id },
			headers: auditHeaders(deleteAuditComment),
		});

		submitting = false;

		if (error) {
			deleteFormError =
				response?.status === 409
					? (error.error ?? t('web.middlewares.delete_in_use'))
					: (error.error ?? t('web.middlewares.delete_failed'));
			return;
		}

		toast.success(t('web.middlewares.deleted'));
		deleteTarget = undefined;
		await invalidateAll();
	}
</script>

<h1 class="mb-2 text-2xl font-semibold">{t('web.middlewares.title')}</h1>
<p class="mb-6 text-hint">{t('web.middlewares.intro')}</p>

{#if data.error}
	<p class="text-destructive">{data.error}</p>
{:else}
	<TableCard
		title={t('web.middlewares.table_title')}
		items={sortedMiddlewares}
		empty={t('web.middlewares.empty')}
		getKey={m => m.id}
	>
		{#snippet header()}
			<th class="px-5 py-3 font-medium">{t('web.common.name')}</th>
			<th class="px-5 py-3 font-medium">{t('web.common.type')}</th>
			<th class="px-5 py-3 font-medium">{t('web.common.id')}</th>
			<th class="px-5 py-3 font-medium"><span class="sr-only">{t('web.common.actions')}</span></th>
		{/snippet}
		{#snippet row(middleware)}
			<td class="px-5 py-4 font-medium">{middleware.name}</td>
			<td class="px-5 py-4">{middleware.type}</td>
			<td class="px-5 py-4">
				<MonoId value={middleware.id} />
			</td>
			<td class="px-5 py-4 text-right">
				<button
					type="button"
					class="btn-ghost"
					disabled={submitting || editLoading}
					onclick={() => openEdit(middleware)}
				>
					{t('web.common.edit')}
				</button>
				<button
					type="button"
					class="btn-ghost text-destructive"
					disabled={submitting || editLoading}
					onclick={() => {
						deleteTarget = middleware;
						deleteAuditComment = '';
						deleteFormError = '';
					}}
				>
					{t('web.common.delete')}
				</button>
			</td>
		{/snippet}
	</TableCard>

	<form class="mt-6 card p-5" onsubmit={handleCreate}>
		<h2 class="mb-1 text-lg font-medium">{t('web.middlewares.create_title')}</h2>
		<p class="mb-4 text-hint">{t('web.middlewares.create_hint')}</p>

		{#if sortedTypes.length === 0}
			<p class="text-hint">{t('web.middlewares.no_types')}</p>
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
						createFieldErrors = {};
						createFormError = '';
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
						fieldErrors={createFieldErrors}
						formError={createFormError}
						{submitting}
						idPrefix="middleware-create"
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
			{/if}

			<div class="mt-4">
				<button type="submit" class="btn-primary" disabled={submitting || !createTypeId || !createName}>
					{submitting ? t('web.common.creating') : t('web.common.create')}
				</button>
			</div>
		{/if}
	</form>
{/if}

<TargetDialog bind:target={editTarget} title="web.middlewares.edit_title">
	{#snippet description(target)}
		{t('web.middlewares.edit_description', target.id)}
	{/snippet}
	{#snippet body(target)}
		{@const targetType = sortedTypes.find(type => type.id === target.type)}
		<form id="middleware-edit-form" class="flex flex-col gap-4" onsubmit={handleEdit}>
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
						fieldErrors={editFieldErrors}
						formError={editFormError}
						{submitting}
						idPrefix="middleware-edit"
					></SchemaForm>
				{/key}
			{:else}
				<p class="text-sm text-destructive" role="alert">{t('web.middlewares.unknown_type')}</p>
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
			formId="middleware-edit-form"
			confirmLabel="web.common.save"
			confirmingLabel="web.common.saving"
			{submitting}
		/>
	{/snippet}
</TargetDialog>

<TargetDialog
	bind:target={deleteTarget}
	title="web.middlewares.delete_title"
	onclose={() => {
		deleteFormError = '';
		deleteAuditComment = '';
	}}
>
	{#snippet description(target)}
		{t('web.middlewares.delete_confirm', target.name ?? '', target.id)}
	{/snippet}
	{#snippet body()}
		<form id="middleware-delete-form" class="flex flex-col gap-4" onsubmit={handleDelete}>
			<label class="flex flex-col gap-1">
				<span class="text-label">{t('web.common.audit_comment')}</span>
				<textarea
					bind:value={deleteAuditComment}
					required={data.auditCommentRequired.delete}
					disabled={submitting}
					rows={3}
					class="input w-full"></textarea>
			</label>
			{#if deleteFormError}
				<p class="text-sm text-destructive" role="alert">{deleteFormError}</p>
			{/if}
		</form>
	{/snippet}
	{#snippet footer()}
		<DialogActions
			formId="middleware-delete-form"
			variant="destructive"
			confirmLabel="web.common.delete"
			confirmingLabel="web.common.deleting"
			{submitting}
		/>
	{/snippet}
</TargetDialog>
