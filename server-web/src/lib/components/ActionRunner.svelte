<script lang="ts">
	import { getClient } from '$lib';
	import { auditHeaders } from '$lib/api';
	import type { components } from '$lib/types/schema';
	import SchemaForm from './SchemaForm.svelte';
	import AuditCommentDialog from './AuditCommentDialog.svelte';

	type ActionResponse = components['schemas']['ActionResponse'];
	type InputFormSchema = components['schemas']['InputFormSchema'];

	let {
		kind,
		targetId
	}: {
		kind: 'user' | 'record';
		targetId: string;
	} = $props();

	let actions = $state<ActionResponse[]>([]);
	let loadError = $state('');
	let loading = $state(true);
	let selected = $state<ActionResponse | null>(null);
	let values = $state<Record<string, string>>({});
	let fieldErrors = $state<Record<string, string>>({});
	let formError = $state('');
	let submitting = $state(false);
	let successMessage = $state('');
	let auditOpen = $state(false);
	let pendingValues = $state<Record<string, string> | null>(null);

	function emptyValues(schema: InputFormSchema) {
		return Object.fromEntries(Object.keys(schema.properties ?? {}).map((key) => [key, '']));
	}

	async function load() {
		loading = true;
		loadError = '';
		successMessage = '';
		const client = getClient();
		const result =
			kind === 'user'
				? await client.GET('/api/user/{id}/actions', { params: { path: { id: targetId } } })
				: await client.GET('/api/records/{id}/actions', { params: { path: { id: targetId } } });

		loading = false;
		if (result.error || !result.data?.data) {
			loadError = result.error?.error ?? 'Failed to load actions';
			actions = [];
			return;
		}
		actions = result.data.data;
	}

	$effect(() => {
		void targetId;
		void kind;
		load();
	});

	function selectAction(action: ActionResponse) {
		selected = action;
		values = emptyValues(action.inputSchema);
		fieldErrors = {};
		formError = '';
		successMessage = '';
	}

	async function runAction(body: Record<string, string>, comment?: string) {
		if (!selected) return;
		submitting = true;
		formError = '';
		fieldErrors = {};
		successMessage = '';

		const headers = auditHeaders(comment);
		const client = getClient();
		const result =
			kind === 'user'
				? await client.POST('/api/user/{id}/actions/{action}', {
						params: { path: { id: targetId, action: selected.id } },
						body,
						headers
					})
				: await client.POST('/api/records/{id}/actions/{action}', {
						params: { path: { id: targetId, action: selected.id } },
						body,
						headers
					});

		submitting = false;

		if (result.error) {
			const err = result.error as { error?: string; errorData?: Record<string, string> };
			if (err.error === 'audit_comment_required' || String(err.errorData ?? '').includes('audit')) {
				pendingValues = body;
				auditOpen = true;
				return;
			}
			if (err.errorData && typeof err.errorData === 'object') {
				fieldErrors = err.errorData;
			}
			formError = err.error ?? 'Action failed';
			return;
		}

		successMessage = `Action “${selected.name}” completed.`;
		selected = null;
		await load();
	}

	async function handleSubmit(formValues: Record<string, string>) {
		if (!selected) return;
		if (selected.requiresAuditComment) {
			pendingValues = formValues;
			auditOpen = true;
			return;
		}
		await runAction(formValues);
	}

	async function onAuditConfirm(comment: string) {
		if (!pendingValues) return;
		const body = pendingValues;
		pendingValues = null;
		await runAction(body, comment);
	}
</script>

<section class="rounded-lg border border-gray-200 p-4 dark:border-gray-700">
	<h2 class="mb-3 text-lg font-semibold">Actions</h2>

	{#if loading}
		<p class="text-sm text-gray-500">Loading actions…</p>
	{:else if loadError}
		<p class="text-sm text-red-600">{loadError}</p>
	{:else if actions.length === 0}
		<p class="text-sm text-gray-500">No actions available.</p>
	{:else}
		<ul class="mb-4 flex flex-wrap gap-2">
			{#each actions as action (action.id)}
				<li>
					<button
						type="button"
						class="rounded border border-gray-300 px-3 py-1.5 text-sm hover:bg-gray-50 dark:border-gray-600"
						onclick={() => selectAction(action)}
					>
						{action.name}
					</button>
				</li>
			{/each}
		</ul>
	{/if}

	{#if successMessage}
		<p class="mb-3 text-sm text-green-700">{successMessage}</p>
	{/if}

	{#if selected}
		<div class="rounded border border-gray-100 p-3 dark:border-gray-800">
			<h3 class="mb-1 font-medium">{selected.name}</h3>
			{#if selected.description}
				<p class="mb-3 text-sm text-gray-600">{selected.description}</p>
			{/if}
			<SchemaForm
				schema={selected.inputSchema}
				bind:values
				bind:fieldErrors
				{submitting}
				{formError}
				submitLabel="Run action"
				onsubmit={handleSubmit}
			/>
		</div>
	{/if}
</section>

<AuditCommentDialog bind:open={auditOpen} onconfirm={onAuditConfirm} />
