<script lang="ts">
	import { FileStoreController } from '$lib/api';
	import type { FileStoreResponse, SimpleFileStoreResponse } from '$lib/api/types.gen';
	import { auditHeaders, getApiClient } from '$lib/api-client';
	import DialogActions from '$lib/components/DialogActions.svelte';
	import MiddlewarePicker from '$lib/components/MiddlewarePicker.svelte';
	import MonoId from '$lib/components/MonoId.svelte';
	import SchemaForm from '$lib/components/SchemaForm.svelte';
	import TableCard from '$lib/components/TableCard.svelte';
	import TargetDialog from '$lib/components/TargetDialog.svelte';
	import { invalidateAll } from '$app/navigation';
	import toast from 'svelte-hot-french-toast';
	import { table } from 'node:console';

	let { data } = $props();

	const sortedStores = $derived(
		[...data.stores].sort((a, b) => {
			const typeCmp = (a.type ?? '').localeCompare(b.type ?? '');
			return typeCmp !== 0 ? typeCmp : (a.id ?? '').localeCompare(b.id ?? '');
		})
	);
	const sortedTypes = $derived([...data.types].sort((a, b) => a.id.localeCompare(b.id)));

	// svelte-ignore state_referenced_locally
	let createTypeId = $state(data.types.at(0)?.id ?? '');
	let createValues = $state<Record<string, string>>({});
	let selectedMiddlewareIds = $state<string[]>([]);
	let createFieldErrors = $state<Record<string, string>>({});
	let createFormError = $state('');
	let createAuditComment = $state('');
	let submitting = $state(false);

	const createType = $derived(sortedTypes.find(type => type.id === createTypeId));

	let editTarget = $state<FileStoreResponse>();
	let editValues = $state<Record<string, string>>({});
	let editFieldErrors = $state<Record<string, string>>({});
	let editFormError = $state('');
	let editAuditComment = $state('');
	let editLoading = $state(false);

	let deleteTarget = $state<SimpleFileStoreResponse>();
	let deleteFormError = $state('');
	let deleteAuditComment = $state('');

	function toFormValues(properties: Record<string, unknown> | undefined): Record<string, string> {
		return Object.fromEntries(
			Object.entries(properties ?? {}).map(([key, value]) => [key, value == null ? '' : String(value)])
		);
	}

	async function handleCreate(event: SubmitEvent) {
		event.preventDefault();
		if (!createTypeId) {
			createFormError = 'Select a file store type.';
			return;
		}

		submitting = true;
		createFieldErrors = {};
		createFormError = '';

		const { error } = await FileStoreController.fileStoreCreate({
			client: getApiClient(),
			body: {
				type: createTypeId,
				properties: createValues,
				middlewares: selectedMiddlewareIds,
			},
			headers: auditHeaders(createAuditComment),
		});

		submitting = false;

		if (error) {
			createFieldErrors = (error.errorData ?? {}) as Record<string, string>;
			createFormError = 'Failed to create file store' + error.error;
			return;
		}

		toast.success('Created file store.');

		createValues = {};
		selectedMiddlewareIds = [];
		createFieldErrors = {};
		createFormError = '';
		createAuditComment = '';

		await invalidateAll();
	}

	async function openEdit(store: SimpleFileStoreResponse) {
		editLoading = true;
		editValues = {};
		editFieldErrors = {};
		editFormError = '';
		editAuditComment = '';

		const { data: result, error } = await FileStoreController.fileStoreRetrieveOne({
			client: getApiClient(),
			path: { id: store.id },
		});

		editLoading = false;

		if (error) {
			toast.error('Failed to load file store: ' + error.error);
			return;
		}

		editTarget = result.data;
		editValues = toFormValues(result.data.properties);
	}

	async function handleEdit(event: SubmitEvent) {
		event.preventDefault();
		if (!editTarget) return;

		submitting = true;
		editFieldErrors = {};
		editFormError = '';

		const { error } = await FileStoreController.fileStoreUpdate({
			client: getApiClient(),
			path: { id: editTarget.id },
			body: {
				properties: editValues,
			},
			headers: auditHeaders(editAuditComment),
		});

		submitting = false;

		if (error) {
			editFieldErrors = (error.errorData ?? {}) as Record<string, string>;
			editFormError = error.error ?? 'Failed to update file store.';
			return;
		}

		toast.success('Updated file store.');
		editTarget = undefined;
		await invalidateAll();
	}

	async function handleDelete(event: SubmitEvent) {
		event.preventDefault();
		if (!deleteTarget) return;

		submitting = true;
		deleteFormError = '';

		const { error } = await FileStoreController.fileStoreDelete({
			client: getApiClient(),
			path: { id: deleteTarget.id },
			headers: auditHeaders(deleteAuditComment),
		});

		submitting = false;

		if (error) {
			deleteFormError = 'Failed to delete file store: ' + error.error;
			return;
		}

		toast.success('Deleted file store.');
		deleteTarget = undefined;
		await invalidateAll();
	}
</script>

<h1 class="mb-2 text-2xl font-semibold">File stores</h1>
<p class="mb-6 text-hint">Create and manage file stores for this workgroup.</p>

{#if data.error}
	<p class="text-destructive">{data.error}</p>
{:else}
	<TableCard title="File stores" items={sortedStores} empty="No file stores are registered." getKey={s => s.id}>
		{#snippet header()}
			<th class="px-5 py-3 font-medium">Type</th>
			<th class="px-5 py-3 font-medium">ID</th>
			<th class="px-5 py-3 font-medium"><span class="sr-only">Actions</span></th>
		{/snippet}
		{#snippet row(store)}
			<td class="px-5 py-4 font-medium">{store.type}</td>
			<td class="px-5 py-4">
				<MonoId value={store.id} />
			</td>
			<td class="px-5 py-4 text-right">
				<button type="button" class="btn-ghost" disabled={submitting || editLoading} onclick={() => openEdit(store)}>
					Edit
				</button>
				<button
					type="button"
					class="btn-ghost text-destructive"
					disabled={submitting || editLoading}
					onclick={() => {
						deleteTarget = store;
						deleteAuditComment = '';
						deleteFormError = '';
					}}
				>
					Delete
				</button>
			</td>
		{/snippet}
	</TableCard>

	<form class="mt-6 card p-5" onsubmit={handleCreate}>
		<h2 class="mb-1 text-lg font-medium">Create file store</h2>
		<p class="mb-4 text-hint">Choose a type, configure its settings, and optionally attach middlewares in order.</p>

		{#if sortedTypes.length === 0}
			<p class="text-hint">No file store types are available.</p>
		{:else}
			<label class="mb-4 flex flex-col gap-1">
				<span class="text-label">Type</span>
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
						idPrefix="file-store-create"
					>
						{#snippet after()}
							<MiddlewarePicker
								middlewares={data.middlewares}
								bind:selected={selectedMiddlewareIds}
								disabled={submitting}
							/>

							<label class="flex flex-col gap-1">
								<span class="text-label">Audit comment</span>
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
				<button type="submit" class="btn-primary" disabled={submitting || !createTypeId}>
					{submitting ? 'Creating…' : 'Create'}
				</button>
			</div>
		{/if}
	</form>
{/if}

<TargetDialog bind:target={editTarget} title="Edit file store" size="wide">
	{#snippet description(target)}
		Update settings for <MonoId value={target.id} />.
	{/snippet}
	{#snippet body(target)}
		{@const targetType = sortedTypes.find(type => type.id === editTarget?.type)}
		<form id="file-store-edit-form" class="flex flex-col gap-4" onsubmit={handleEdit}>
			<span class="flex flex-col gap-1">There are currently {target.fileCount} files in this store.</span>

			<label class="flex flex-col gap-1">
				<span class="text-label">Type</span>
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
						idPrefix="file-store-edit"
					></SchemaForm>
				{/key}
			{:else}
				<p class="text-sm text-destructive" role="alert">Unknown file store type.</p>
			{/if}

			<div class="flex flex-col gap-1">
				<span class="text-label">Middlewares</span>
				{#if target.middlewares.length === 0}
					<p class="text-hint">None</p>
				{:else}
					<ul class="flex flex-col gap-2">
						{#each target.middlewares as id (id)}
							{@const middleware = data.middlewares.find(m => m.id === id)}
							<li>
								<span class="font-medium">{middleware?.type ?? 'Unknown'}</span>
								<span class="block"><MonoId value={id} muted /></span>
							</li>
						{/each}
					</ul>
				{/if}
			</div>

			<label class="flex flex-col gap-1">
				<span class="text-label">Audit comment</span>
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
		<DialogActions formId="file-store-edit-form" confirmLabel="Save" confirmingLabel="Saving…" {submitting} />
	{/snippet}
</TargetDialog>

<TargetDialog
	bind:target={deleteTarget}
	title="Delete file store"
	onclose={() => {
		deleteFormError = '';
		deleteAuditComment = '';
	}}
>
	{#snippet description(target)}
		Remove store <MonoId value={target.id} /> ({target.type})? This fails if the store still has files.
	{/snippet}
	{#snippet body()}
		<form id="file-store-delete-form" class="flex flex-col gap-4" onsubmit={handleDelete}>
			<label class="flex flex-col gap-1">
				<span class="text-label">Audit comment</span>
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
			formId="file-store-delete-form"
			variant="destructive"
			confirmLabel="Delete"
			confirmingLabel="Deleting…"
			{submitting}
		/>
	{/snippet}
</TargetDialog>
