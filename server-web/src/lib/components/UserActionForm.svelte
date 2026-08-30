<script lang="ts">
	import type { ActionResponse, InputFormSchema } from '$lib/api/types.gen';
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
	let successMessage = $state('');
	let submitting = $state(false);

	async function handleSubmit() {
		if (action.requiresAuditComment && !auditComment.trim()) {
			formError = 'An audit comment is required for this action.';
			return;
		}

		submitting = true;
		fieldErrors = {};
		formError = '';
		successMessage = '';

		const { error } = await UserController.executeAction({
			path: { id: userId, action: action.id },
			body: values,
			headers: action.requiresAuditComment
				? { 'X-ORM-Audit-Comment': auditComment.trim() }
				: undefined
		});

		submitting = false;

		if (error) {
			fieldErrors = (error.errorData ?? {}) as Record<string, string>;
			formError = error.error ?? 'Action failed';
			return;
		}

		values = {};
		successMessage = `${action.name} completed successfully.`;
		auditComment = '';
		onsuccess?.();
	}
</script>

<section class="rounded-lg border border-gray-200 p-4 dark:border-gray-700">
	<h2 class="text-lg font-medium">{action.name}</h2>
	{#if action.description}
		<p class="mt-1 text-sm text-gray-600 dark:text-gray-300">{action.description}</p>
	{/if}

	<div class="mt-4">
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
						rows="2"
						class="rounded border border-gray-300 px-3 py-2 dark:border-gray-600"
					></textarea>
				</label>
			{/snippet}
		</SchemaForm>
	</div>

	{#if successMessage}
		<p class="mt-3 text-sm text-green-700 dark:text-green-400" role="status">{successMessage}</p>
	{/if}
</section>
