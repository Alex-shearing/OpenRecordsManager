<script lang="ts">
	import { FileStoreController } from '$lib/api';
	import type { SimpleFileStoreResponse } from '$lib/api/types.gen';
	import { getApiClient } from '$lib/api-client';
	import DialogActions from '$lib/components/DialogActions.svelte';
	import MonoId from '$lib/components/MonoId.svelte';
	import SchemaForm from '$lib/components/SchemaForm.svelte';
	import TableCard from '$lib/components/TableCard.svelte';
	import TargetDialog from '$lib/components/TargetDialog.svelte';
	import { invalidateAll } from '$app/navigation';
	import toast from 'svelte-hot-french-toast';

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

	let editTarget = $state<SimpleFileStoreResponse | null>(null);
	let editTypeId = $state('');
	let editMiddlewares = $state<string[]>([]);
	let editValues = $state<Record<string, string>>({});
	let editFieldErrors = $state<Record<string, string>>({});
	let editFormError = $state('');
	let editAuditComment = $state('');
	let editLoading = $state(false);

	const editType = $derived(sortedTypes.find(type => type.id === editTypeId));

	let deleteTarget = $state<SimpleFileStoreResponse | null>(null);
	let deleteFormError = $state('');
	let deleteAuditComment = $state('');

	function toFormValues(properties: Record<string, unknown> | undefined): Record<string, string> {
		return Object.fromEntries(
			Object.entries(properties ?? {}).map(([key, value]) => [key, value == null ? '' : String(value)])
		);
	}

	function auditHeaders(comment: string) {
		return comment.trim() ? { 'X-ORM-Audit-Comment': comment.trim() } : undefined;
	}

	function resetCreateForm() {
		createValues = {};
		selectedMiddlewareIds = [];
		createFieldErrors = {};
		createFormError = '';
		createAuditComment = '';
	}

	function onCreateTypeChange() {
		createValues = {};
		createFieldErrors = {};
		createFormError = '';
	}

	function toggleMiddleware(target: 'create' | 'edit', id: string, checked: boolean) {
		const current = target === 'create' ? selectedMiddlewareIds : editMiddlewares;
		const next = checked
			? current.includes(id)
				? current
				: [...current, id]
			: current.filter(middlewareId => middlewareId !== id);

		if (target === 'create') {
			selectedMiddlewareIds = next;
		} else {
			editMiddlewares = next;
		}
	}

	async function handleCreate(event: SubmitEvent) {
		event.preventDefault();

		if (!createTypeId) {
			createFormError = 'Select a file store type.';
			return;
		}

		if (data.auditCommentRequired.create && !createAuditComment.trim()) {
			createFormError = 'An audit comment is required for this action.';
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
			createFormError = error.error ?? 'Failed to create file store.';
			return;
		}

		toast.success('Created file store.');
		resetCreateForm();
		await invalidateAll();
	}

	async function openEdit(store: SimpleFileStoreResponse) {
		editTarget = store;
		editTypeId = store.type;
		editValues = {};
		editMiddlewares = [];
		editFieldErrors = {};
		editFormError = '';
		editAuditComment = '';
		editLoading = true;

		const { data: result, error } = await FileStoreController.fileStoreRetrieveOne({
			client: getApiClient(),
			path: { id: store.id },
		});

		editLoading = false;

		if (error || !result?.success || !result.data) {
			editFormError = error?.error ?? 'Failed to load file store.';
			return;
		}

		const detail = result.data;
		editTypeId = detail.type;
		editMiddlewares = detail.middlewares ?? [];
		editValues = toFormValues(detail.properties);
	}

	async function handleEdit(event: SubmitEvent) {
		event.preventDefault();

		if (!editTarget) {
			return;
		}

		if (data.auditCommentRequired.update && !editAuditComment.trim()) {
			editFormError = 'An audit comment is required for this action.';
			return;
		}

		submitting = true;
		editFieldErrors = {};
		editFormError = '';

		const { error } = await FileStoreController.fileStoreUpdate({
			client: getApiClient(),
			path: { id: editTarget.id },
			body: {
				properties: editValues,
				middlewares: editMiddlewares,
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
		editTarget = null;
		await invalidateAll();
	}

	function openDelete(store: SimpleFileStoreResponse) {
		deleteTarget = store;
		deleteFormError = '';
		deleteAuditComment = '';
	}

	async function confirmDelete() {
		if (!deleteTarget) {
			return;
		}

		if (data.auditCommentRequired.delete && !deleteAuditComment.trim()) {
			deleteFormError = 'An audit comment is required for this action.';
			return;
		}

		submitting = true;
		deleteFormError = '';

		const { error } = await FileStoreController.fileStoreDelete({
			client: getApiClient(),
			path: { id: deleteTarget.id },
			headers: auditHeaders(deleteAuditComment),
		});

		submitting = false;

		if (error) {
			deleteFormError = error.error ?? 'Failed to delete file store.';
			return;
		}

		toast.success('Deleted file store.');
		deleteTarget = null;
		await invalidateAll();
	}
</script>

<h1 class="mb-2 text-2xl font-semibold">File stores</h1>
<p class="mb-6 text-hint">
	Create and manage file stores for this workgroup. Attach middlewares when creating or editing a store; checked order
	is the application order.
</p>

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
				<button
					type="button"
					class="btn-ghost"
					disabled={submitting || submitting || submitting}
					onclick={() => openEdit(store)}
				>
					Edit
				</button>
				<button
					type="button"
					class="btn-ghost text-destructive"
					disabled={submitting || submitting || submitting}
					onclick={() => openDelete(store)}
				>
					Delete
				</button>
			</td>
		{/snippet}
	</TableCard>

	<form class="mt-6 card p-5" onsubmit={handleCreate} novalidate>
		<h2 class="mb-1 text-lg font-medium">Create file store</h2>
		<p class="mb-4 text-hint">Choose a type, configure its settings, and optionally attach middlewares in order.</p>

		{#if sortedTypes.length === 0}
			<p class="text-hint">No file store types are available.</p>
		{:else}
			<label class="mb-4 flex flex-col gap-1">
				<span class="text-label">Type</span>
				<select
					class="input w-full max-w-md"
					bind:value={createTypeId}
					disabled={submitting}
					onchange={onCreateTypeChange}
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
							{#if data.middlewares.length > 0}
								<fieldset>
									<legend class="mb-2 text-label">Middlewares</legend>
									<p class="mb-2 text-hint">Checked order is the application order.</p>
									<ul class="flex flex-col gap-2">
										{#each data.middlewares as middleware (middleware.id)}
											<li>
												<label class="flex items-start gap-2 text-sm">
													<input
														type="checkbox"
														class="mt-0.5 size-4 rounded border-border-input"
														disabled={submitting}
														checked={selectedMiddlewareIds.includes(middleware.id)}
														onchange={event =>
															toggleMiddleware(
																'create',
																middleware.id,
																(event.currentTarget as HTMLInputElement).checked
															)}
													/>
													<span>
														<span class="font-medium">{middleware.type}</span>
														<span class="block"><MonoId value={middleware.id} muted /></span>
													</span>
												</label>
											</li>
										{/each}
									</ul>
								</fieldset>
							{/if}

							<label class="flex flex-col gap-1">
								<span class="text-label">Audit comment</span>
								<textarea
									bind:value={createAuditComment}
									required={data.auditCommentRequired.create}
									disabled={submitting}
									rows={3}
									class="input w-full max-w-md"></textarea>
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

<TargetDialog bind:target={editTarget} title="Edit file store">
	{#snippet description(target)}
		Update settings for <MonoId value={target.id} />.
	{/snippet}
	{#snippet body()}
		{#if editLoading}
			<p class="text-hint">Loading…</p>
		{:else}
			<form id="file-store-edit-form" class="flex flex-col gap-4" onsubmit={handleEdit} novalidate>
				<label class="flex flex-col gap-1">
					<span class="text-label">Type</span>
					<input class="input" value={editTypeId} readonly disabled />
				</label>

				{#if data.middlewares.length > 0}
					<fieldset>
						<legend class="mb-2 text-label">Middlewares</legend>
						<p class="mb-2 text-hint">Checked order is the application order.</p>
						<ul class="flex flex-col gap-2">
							{#each data.middlewares as middleware (middleware.id)}
								<li>
									<label class="flex items-start gap-2 text-sm">
										<input
											type="checkbox"
											class="mt-0.5 size-4 rounded border-border-input"
											disabled={submitting}
											checked={editMiddlewares.includes(middleware.id)}
											onchange={event =>
												toggleMiddleware('edit', middleware.id, (event.currentTarget as HTMLInputElement).checked)}
										/>
										<span>
											<span class="font-medium">{middleware.type}</span>
											<span class="block"><MonoId value={middleware.id} muted /></span>
										</span>
									</label>
								</li>
							{/each}
						</ul>
					</fieldset>
				{:else}
					<div>
						<p class="text-label">Middlewares</p>
						<p class="text-hint">None available</p>
					</div>
				{/if}

				{#if editType}
					{#key editTarget?.id}
						<SchemaForm
							schema={editType.settingsSchema}
							bind:values={editValues}
							fieldErrors={editFieldErrors}
							formError={editFormError}
							{submitting}
							idPrefix="file-store-edit"
						>
							{#snippet after()}
								<label class="flex flex-col gap-1">
									<span class="text-label">Audit comment</span>
									<textarea
										bind:value={editAuditComment}
										required={data.auditCommentRequired.update}
										disabled={submitting}
										rows={3}
										class="input w-full"></textarea>
								</label>
							{/snippet}
						</SchemaForm>
					{/key}
				{:else if editFormError}
					<p class="text-sm text-destructive" role="alert">{editFormError}</p>
				{/if}
			</form>
		{/if}
	{/snippet}
	{#snippet footer()}
		<DialogActions
			formId="file-store-edit-form"
			confirmLabel="Save"
			confirmingLabel="Saving…"
			submitting={submitting || editLoading}
			disabled={editLoading || !editType}
		/>
	{/snippet}
</TargetDialog>

<TargetDialog bind:target={deleteTarget} title="Delete file store" onclose={() => (deleteFormError = '')}>
	{#snippet description(target)}
		Remove store <MonoId value={target.id} /> ({target.type})? This fails if the store still has files.
	{/snippet}
	{#snippet body()}
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
			<p class="mt-3 text-sm text-destructive" role="alert">{deleteFormError}</p>
		{/if}
	{/snippet}
	{#snippet footer()}
		<DialogActions
			variant="destructive"
			confirmLabel="Delete"
			confirmingLabel="Deleting…"
			{submitting}
			onconfirm={confirmDelete}
		/>
	{/snippet}
</TargetDialog>
