<script lang="ts">
	import type { ActionResponse } from '$lib/api/types.gen';
	import { UserController } from '$lib/api';
	import { getApiClient } from '$lib/api-client';
	import AppDialog from './AppDialog.svelte';
	import DialogActions from './DialogActions.svelte';
	import SchemaForm from './SchemaForm.svelte';

	const formId = 'user-action-dialog-form';

	let {
		open = $bindable(false),
		userId,
		action,
		onclose,
	}: {
		open?: boolean;
		userId: string;
		action: ActionResponse | null;
		onclose?: () => void;
	} = $props();

	let values = $state<Record<string, string>>({});
	let auditComment = $state('');
	let fieldErrors = $state<Record<string, string>>({});
	let formError = $state('');
	let submitting = $state(false);

	function handleClose() {
		open = false;
		onclose?.();
	}

	async function handleSubmit(event: SubmitEvent) {
		event.preventDefault();

		if (!action) {
			return;
		}

		if (action.requiresAuditComment && !auditComment.trim()) {
			formError = 'An audit comment is required for this action.';
			return;
		}

		submitting = true;
		fieldErrors = {};
		formError = '';

		const { error } = await UserController.executeAction({
			client: getApiClient(),
			path: { id: userId, action: action.id },
			body: values,
			headers: auditComment.trim() ? { 'X-ORM-Audit-Comment': auditComment.trim() } : undefined,
		});

		submitting = false;

		if (error) {
			fieldErrors = (error.errorData ?? {}) as Record<string, string>;
			formError = error.error ?? 'Action failed';
			return;
		}

		handleClose();
	}
</script>

{#if action}
	{#key action.id}
		<AppDialog bind:open={open} title={action.name} onclose={handleClose}>
			{#snippet description()}
				{#if action.description}
					{action.description}
				{/if}
			{/snippet}
			{#snippet body()}
				<form id={formId} class="flex flex-col gap-4" onsubmit={handleSubmit} novalidate>
					<SchemaForm
						schema={action.inputSchema}
						bind:values
						{fieldErrors}
						{formError}
						{submitting}
						idPrefix="action-{action.id}"
					>
						{#snippet after()}
							<label class="flex flex-col gap-1">
								<span class="text-label">Audit comment</span>
								<textarea
									bind:value={auditComment}
									required={action.requiresAuditComment}
									disabled={submitting}
									rows={3}
									class="input w-full"
								></textarea>
							</label>
						{/snippet}
					</SchemaForm>
				</form>
			{/snippet}
			{#snippet footer()}
				<DialogActions
					{formId}
					confirmLabel={action.name}
					confirmingLabel="Working…"
					{submitting}
				/>
			{/snippet}
		</AppDialog>
	{/key}
{/if}
