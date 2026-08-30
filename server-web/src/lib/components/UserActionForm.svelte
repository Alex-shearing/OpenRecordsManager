<script lang="ts">
	import type { ActionResponse } from '$lib/api/types.gen';
	import { UserController } from '$lib';
	import SchemaForm from './SchemaForm.svelte';

	let {
		userId,
		action,
		onsuccess
	}: {
		userId: string;
		action: ActionResponse;
		onsuccess?: () => void;
	} = $props();

	let values = $state<Record<string, string>>({});
	let auditComment = $state('');
	let fieldErrors = $state<Record<string, string>>({});
	let formError = $state('');
	let submitting = $state(false);

	async function handleSubmit() {
		if (action.requiresAuditComment && !auditComment.trim()) {
			formError = 'An audit comment is required for this action.';
			return;
		}

		submitting = true;
		fieldErrors = {};
		formError = '';

		const { error } = await UserController.executeAction({
			path: { id: userId, action: action.id },
			body: values,
			headers: auditComment.trim()
				? { 'X-ORM-Audit-Comment': auditComment.trim() }
				: undefined
		});

		submitting = false;

		if (error) {
			fieldErrors = (error.errorData ?? {}) as Record<string, string>;
			formError = error.error ?? 'Action failed';
			return;
		}

		onsuccess?.();
	}
</script>

<SchemaForm
	schema={action.inputSchema}
	bind:values
	{fieldErrors}
	{formError}
	{submitting}
	submitLabel={action.name}
	submittingLabel="Working..."
	idPrefix="action-{action.id}"
	onsubmit={handleSubmit}
>
	{#snippet after()}
		<label class="flex flex-col gap-1">
			<span>Audit comment</span>
			<textarea
				bind:value={auditComment}
				required={action.requiresAuditComment}
				disabled={submitting}
				rows="3"
				class="rounded border border-gray-300 px-3 py-2 dark:border-gray-600"
			></textarea>
		</label>
	{/snippet}
</SchemaForm>
