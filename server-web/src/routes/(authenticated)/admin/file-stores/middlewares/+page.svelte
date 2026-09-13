<script lang="ts">
	import { FileStoreController } from '$lib/api';
	import type { SimpleMiddlewareResponse } from '$lib/api/types.gen';
	import { getApiClient } from '$lib/api-client';
	import DialogActions from '$lib/components/DialogActions.svelte';
	import MonoId from '$lib/components/MonoId.svelte';
	import SchemaForm from '$lib/components/SchemaForm.svelte';
	import TableCard from '$lib/components/TableCard.svelte';
	import TargetDialog from '$lib/components/TargetDialog.svelte';
	import { invalidateAll } from '$app/navigation';
	import toast from 'svelte-hot-french-toast';

	let { data } = $props();

	const sortedMiddlewares = $derived(
		[...data.middlewares].sort((a, b) => a.type.localeCompare(b.type) || a.id.localeCompare(b.id))
	);
	const sortedTypes = $derived([...data.types].sort((a, b) => a.id.localeCompare(b.id)));

	// svelte-ignore state_referenced_locally
	let createTypeId = $state(data.types.at(0)?.id ?? '');
	let createValues = $state<Record<string, string>>({});
	let createFieldErrors = $state<Record<string, string>>({});
	let createFormError = $state('');
	let createAuditComment = $state('');
	let submitting = $state(false);

	let editTarget = $state<SimpleMiddlewareResponse | null>(null);
	let editTypeId = $state('');
	let editValues = $state<Record<string, string>>({});
	let editFieldErrors = $state<Record<string, string>>({});
	let editFormError = $state('');
	let editAuditComment = $state('');
	let editLoading = $state(false);

	let deleteTarget = $state<SimpleMiddlewareResponse | null>(null);
	let deleteFormError = $state('');
	let deleteAuditComment = $state('');

	const createType = $derived(sortedTypes.find(type => type.id === createTypeId));
	const editType = $derived(sortedTypes.find(type => type.id === editTypeId));

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
		createFieldErrors = {};
		createFormError = '';
		createAuditComment = '';
	}

	async function handleCreate(event: SubmitEvent) {
		event.preventDefault();
		if (!createTypeId) {
			createFormError = 'Select a middleware type.';
			return;
		}

		submitting = true;
		createFieldErrors = {};
		createFormError = '';

		const { error } = await FileStoreController.middlewareCreate({
			client: getApiClient(),
			body: {
				type: createTypeId,
				properties: createValues,
			},
			headers: auditHeaders(createAuditComment),
		});

		submitting = false;

		if (error) {
			createFieldErrors = (error.errorData ?? {}) as Record<string, string>;
			createFormError = error.error ?? 'Failed to create middleware.';
			return;
		}

		toast.success('Created middleware.');
		resetCreateForm();
		await invalidateAll();
	}

	async function openEdit(middleware: SimpleMiddlewareResponse) {
		editTarget = middleware;
		editTypeId = middleware.type;
		editValues = {};
		editFieldErrors = {};
		editFormError = '';
		editAuditComment = '';
		editLoading = true;

		const { data: result, error } = await FileStoreController.middlewareRetrieveOne({
			client: getApiClient(),
			path: { id: middleware.id },
		});

		editLoading = false;

		if (error || !result?.success || !result.data) {
			editFormError = error?.error ?? 'Failed to load middleware.';
			return;
		}

		editTypeId = result.data.type;
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
			body: editValues,
			headers: auditHeaders(editAuditComment),
		});

		submitting = false;

		if (error) {
			editFieldErrors = (error.errorData ?? {}) as Record<string, string>;
			editFormError = error.error ?? 'Failed to update middleware.';
			return;
		}

		toast.success('Updated middleware.');
		editTarget = null;
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
					? (error.error ?? 'This middleware is in use by one or more file stores and cannot be deleted.')
					: (error.error ?? 'Failed to delete middleware.');
			return;
		}

		toast.success('Deleted middleware.');
		deleteTarget = null;
		await invalidateAll();
	}
</script>

<h1 class="mb-2 text-2xl font-semibold">File store middlewares</h1>
<p class="mb-6 text-hint">Create reusable middleware configurations that can be attached when creating a file store.</p>

{#if data.error}
	<p class="text-destructive">{data.error}</p>
{:else}
	<TableCard
		title="File store middlewares"
		items={sortedMiddlewares}
		empty="No middlewares are registered."
		getKey={m => m.id}
	>
		{#snippet header()}
			<th class="px-5 py-3 font-medium">Type</th>
			<th class="px-5 py-3 font-medium">ID</th>
			<th class="px-5 py-3 font-medium"><span class="sr-only">Actions</span></th>
		{/snippet}
		{#snippet row(middleware)}
			<td class="px-5 py-4 font-medium">{middleware.type}</td>
			<td class="px-5 py-4">
				<MonoId value={middleware.id} />
			</td>
			<td class="px-5 py-4 text-right">
				<button type="button" class="btn-ghost" disabled={submitting} onclick={() => openEdit(middleware)}>
					Edit
				</button>
				<button
					type="button"
					class="btn-ghost text-destructive"
					disabled={submitting}
					onclick={() => {
						deleteTarget = middleware;
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
		<h2 class="mb-1 text-lg font-medium">Create middleware</h2>
		<p class="mb-4 text-hint">Choose a type and configure its properties.</p>

		{#if sortedTypes.length === 0}
			<p class="text-hint">No middleware types are available.</p>
		{:else}
			<label class="mb-4 flex flex-col gap-1">
				<span class="text-label">Type</span>
				<select
					class="input w-full max-w-md"
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
								<span class="text-label">Audit comment</span>
								<textarea
									bind:value={createAuditComment}
									required={data.auditCommentRequired.create}
									disabled={submitting}
									rows={3}
									class="input w-full max-w-md"
								></textarea>
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

<TargetDialog bind:target={editTarget} title="Edit file store middleware">
	{#snippet description(target)}
		Update properties for <MonoId value={target.id} />.
	{/snippet}
	{#snippet body()}
		{#if editLoading}
			<p class="text-hint">Loading…</p>
		{:else}
			<form id="middleware-edit-form" class="flex flex-col gap-4" onsubmit={handleEdit}>
				<label class="flex flex-col gap-1">
					<span class="text-label">Type</span>
					<input class="input" value={editTypeId} readonly disabled />
				</label>

				{#if editType}
					{#key editTarget?.id}
						<SchemaForm
							schema={editType.settingsSchema}
							bind:values={editValues}
							fieldErrors={editFieldErrors}
							formError={editFormError}
							{submitting}
							idPrefix="middleware-edit"
						>
							{#snippet after()}
								<label class="flex flex-col gap-1">
									<span class="text-label">Audit comment</span>
									<textarea
										bind:value={editAuditComment}
										required={data.auditCommentRequired.update}
										disabled={submitting}
										rows={3}
										class="input w-full"
									></textarea>
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
			formId="middleware-edit-form"
			confirmLabel="Save"
			confirmingLabel="Saving…"
			submitting={submitting || editLoading}
			disabled={editLoading || !editType}
		/>
	{/snippet}
</TargetDialog>

<TargetDialog
	bind:target={deleteTarget}
	title="Delete file store middleware"
	onclose={() => {
		deleteFormError = '';
		deleteAuditComment = '';
	}}
>
	{#snippet description(target)}
		Remove <span class="font-medium">{target.type}</span> (<MonoId value={target.id} />)? This fails if it is attached
		to a file store.
	{/snippet}
	{#snippet body()}
		<form id="middleware-delete-form" class="flex flex-col gap-4" onsubmit={handleDelete}>
			<label class="flex flex-col gap-1">
				<span class="text-label">Audit comment</span>
				<textarea
					bind:value={deleteAuditComment}
					required={data.auditCommentRequired.delete}
					disabled={submitting}
					rows={3}
					class="input w-full"
				></textarea>
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
			confirmLabel="Delete"
			confirmingLabel="Deleting…"
			{submitting}
		/>
	{/snippet}
</TargetDialog>
