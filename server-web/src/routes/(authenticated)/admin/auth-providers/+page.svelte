<script lang="ts">
	import { AuthController } from '$lib/api';
	import type { AuthProviderResponse, SimpleAuthProviderResponse } from '$lib/api/types.gen';
	import { auditHeaders, getApiClient } from '$lib/api-client';
	import DialogActions from '$lib/components/DialogActions.svelte';
	import MonoId from '$lib/components/MonoId.svelte';
	import SchemaForm from '$lib/components/SchemaForm.svelte';
	import TableCard from '$lib/components/TableCard.svelte';
	import TargetDialog from '$lib/components/TargetDialog.svelte';
	import { invalidateAll } from '$app/navigation';
	import toast from 'svelte-hot-french-toast';

	let { data } = $props();

	const sortedProviders = $derived(
		[...data.providers].sort((a, b) => {
			const nameCmp = (a.name ?? '').localeCompare(b.name ?? '');
			return nameCmp !== 0 ? nameCmp : (a.id ?? '').localeCompare(b.id ?? '');
		})
	);
	const sortedTypes = $derived([...data.types].sort((a, b) => a.type.id.localeCompare(b.type.id)));

	// svelte-ignore state_referenced_locally
	let createTypeId = $state(data.types.at(0)?.type.id ?? '');
	let createName = $state('');
	let createValues = $state<Record<string, string>>({});
	let createFieldErrors = $state<Record<string, string>>({});
	let createFormError = $state('');
	let createAuditComment = $state('');
	let submitting = $state(false);

	const createType = $derived(sortedTypes.find(type => type.type.id === createTypeId));

	let editTarget = $state<AuthProviderResponse>();
	let editName = $state('');
	let editEnabled = $state(true);
	let editValues = $state<Record<string, string>>({});
	let editFieldErrors = $state<Record<string, string>>({});
	let editFormError = $state('');
	let editAuditComment = $state('');
	let editLoading = $state(false);

	const editType = $derived(editTarget ? sortedTypes.find(type => type.type.id === editTarget?.type.id) : undefined);

	function toFormValues(properties: Record<string, unknown> | undefined): Record<string, string> {
		return Object.fromEntries(
			Object.entries(properties ?? {}).map(([key, value]) => [key, value == null ? '' : String(value)])
		);
	}

	async function handleCreate(event: SubmitEvent) {
		event.preventDefault();
		if (!createType) {
			createFormError = 'Select a provider type.';
			return;
		}

		submitting = true;
		createFieldErrors = {};
		createFormError = '';

		const { data: result, error } = await AuthController.createAuthProvider({
			client: getApiClient(),
			body: {
				name: createName,
				type: createType.type,
				settings: createType.settingsSchema ? createValues : {},
			},
			headers: auditHeaders(createAuditComment),
		});

		submitting = false;

		if (error) {
			createFieldErrors = (error.errorData ?? {}) as Record<string, string>;
			createFormError = 'Failed to create login provider: ' + error.error;
			return;
		}

		toast.success('Created login provider.');

		const created = result?.data;
		if (createType.type.type === 'redirect_auth_provider' && created?.id) {
			toast.success(`Callback URL: ${window.location.origin}/api/auth/callback/${created.id}`, {
				duration: 10000,
			});
		}

		createName = '';
		createValues = {};
		createFieldErrors = {};
		createFormError = '';
		createAuditComment = '';

		await invalidateAll();
	}

	async function openEdit(provider: SimpleAuthProviderResponse) {
		editLoading = true;
		editValues = {};
		editFieldErrors = {};
		editFormError = '';
		editAuditComment = '';

		const { data: result, error } = await AuthController.getAuthProvider({
			client: getApiClient(),
			path: { id: provider.id },
		});

		editLoading = false;

		if (error) {
			toast.error('Failed to load login provider: ' + error.error);
			return;
		}

		editTarget = result.data;
		editName = result.data.name;
		editEnabled = result.data.enabled;
		editValues = toFormValues(result.data.settings);
	}

	async function handleEdit(event: SubmitEvent) {
		event.preventDefault();
		if (!editTarget) return;

		submitting = true;
		editFieldErrors = {};
		editFormError = '';

		const { error } = await AuthController.updateAuthProvider({
			client: getApiClient(),
			path: { id: editTarget.id },
			body: {
				name: editName,
				enabled: editEnabled,
				settings: editType?.settingsSchema ? editValues : undefined,
			},
			headers: auditHeaders(editAuditComment),
		});

		submitting = false;

		if (error) {
			editFieldErrors = (error.errorData ?? {}) as Record<string, string>;
			editFormError = error.error ?? 'Failed to update login provider.';
			return;
		}

		toast.success('Updated login provider.');
		editTarget = undefined;
		await invalidateAll();
	}
</script>

<h1 class="mb-2 text-2xl font-semibold">Login providers</h1>
<p class="mb-6 text-hint">Create and manage authentication providers for this workgroup.</p>

{#if data.error}
	<p class="text-destructive">{data.error}</p>
{:else}
	<TableCard
		title="Login providers"
		items={sortedProviders}
		empty="No login providers are registered."
		getKey={p => p.id}
	>
		{#snippet header()}
			<th class="px-5 py-3 font-medium">Name</th>
			<th class="px-5 py-3 font-medium">Type</th>
			<th class="px-5 py-3 font-medium">Enabled</th>
			<th class="px-5 py-3 font-medium">ID</th>
			<th class="px-5 py-3 font-medium"><span class="sr-only">Actions</span></th>
		{/snippet}
		{#snippet row(provider)}
			<td class="px-5 py-4 font-medium">{provider.name}</td>
			<td class="px-5 py-4">{provider.type.id}</td>
			<td class="px-5 py-4">{provider.enabled ? 'Yes' : 'No'}</td>
			<td class="px-5 py-4">
				<MonoId value={provider.id} />
			</td>
			<td class="px-5 py-4 text-right">
				<button type="button" class="btn-ghost" disabled={submitting || editLoading} onclick={() => openEdit(provider)}>
					Edit
				</button>
			</td>
		{/snippet}
	</TableCard>

	<form class="mt-6 card p-5" onsubmit={handleCreate}>
		<h2 class="mb-1 text-lg font-medium">Create login provider</h2>
		<p class="mb-4 text-hint">Choose a type, configure its settings, and give it a name.</p>

		{#if sortedTypes.length === 0}
			<p class="text-hint">No login provider types are available.</p>
		{:else}
			<label class="mb-4 flex flex-col gap-1">
				<span class="text-label">Name</span>
				<input class="input w-full" bind:value={createName} required disabled={submitting} />
			</label>

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
					{#each sortedTypes as type (type.type.id)}
						<option value={type.type.id}>{type.type.id}</option>
					{/each}
				</select>
			</label>

			{#if createType}
				{#if createType.settingsSchema}
					{#key createTypeId}
						<SchemaForm
							schema={createType.settingsSchema}
							bind:values={createValues}
							fieldErrors={createFieldErrors}
							formError={createFormError}
							{submitting}
							idPrefix="auth-provider-create"
						>
							{#snippet after()}
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
				{:else}
					<p class="mb-4 text-hint">This provider type has no settings.</p>
					{#if createFormError}
						<p class="mb-4 text-sm text-destructive" role="alert">{createFormError}</p>
					{/if}
					<label class="mb-4 flex flex-col gap-1">
						<span class="text-label">Audit comment</span>
						<textarea
							bind:value={createAuditComment}
							required={data.auditCommentRequired.create}
							disabled={submitting}
							rows={3}
							class="input w-full"></textarea>
					</label>
				{/if}
			{/if}

			<div class="mt-4">
				<button type="submit" class="btn-primary" disabled={submitting || !createTypeId || !createName}>
					{submitting ? 'Creating…' : 'Create'}
				</button>
			</div>
		{/if}
	</form>
{/if}

<TargetDialog bind:target={editTarget} title="Edit login provider" size="wide">
	{#snippet description(target)}
		Update settings for <MonoId value={target.id} />.
	{/snippet}
	{#snippet body(target)}
		<form id="auth-provider-edit-form" class="flex flex-col gap-4" onsubmit={handleEdit}>
			<label class="flex flex-col gap-1">
				<span class="text-label">Name</span>
				<input class="input w-full" bind:value={editName} required disabled={submitting} />
			</label>

			<label class="flex items-center gap-2">
				<input type="checkbox" bind:checked={editEnabled} disabled={submitting} />
				<span class="text-label">Enabled</span>
			</label>

			<label class="flex flex-col gap-1">
				<span class="text-label">Type</span>
				<input class="input w-full" value={target.type.id} readonly disabled />
			</label>

			{#if editType?.settingsSchema}
				{#key target.id}
					<SchemaForm
						schema={editType.settingsSchema}
						bind:values={editValues}
						fieldErrors={editFieldErrors}
						formError={editFormError}
						{submitting}
						idPrefix="auth-provider-edit"
					></SchemaForm>
				{/key}
			{:else}
				<p class="text-hint">This provider type has no settings.</p>
				{#if editFormError}
					<p class="text-sm text-destructive" role="alert">{editFormError}</p>
				{/if}
			{/if}

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
		<DialogActions formId="auth-provider-edit-form" confirmLabel="Save" confirmingLabel="Saving…" {submitting} />
	{/snippet}
</TargetDialog>
