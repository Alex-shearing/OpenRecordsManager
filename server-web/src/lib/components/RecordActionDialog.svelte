<script lang="ts">
	import type { ActionResponse } from '#lib/api/types.gen.js';
	import { RecordController } from '#lib/api/index.js';
	import { auditHeaders, getApiClient } from '#lib/api-client.js';
	import { t } from '#lib/i18n/catalog.js';
	import { recordActionDescription } from '#lib/i18n/labels.js';
	import AppDialog from './AppDialog.svelte';
	import DialogActions from './DialogActions.svelte';
	import SchemaForm from './SchemaForm.svelte';
	import type { SchemaFormError } from './SchemaForm.svelte';

	const formId = 'record-action-dialog-form';

	let {
		open = $bindable(false),
		recordId,
		action,
		onclose,
	}: {
		open?: boolean;
		recordId: string;
		action?: ActionResponse;
		onclose?: () => void;
	} = $props();

	let values = $state<Record<string, string>>({});
	let auditComment = $state('');
	let error = $state<SchemaFormError>();
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
			error = { error: 'audit_comment_required' };
			return;
		}

		submitting = true;
		error = undefined;

		const { error: apiError } = await RecordController.executeAction({
			client: getApiClient(),
			path: { id: recordId, action: action.id },
			body: values,
			headers: auditHeaders(auditComment),
		});

		submitting = false;

		if (apiError) {
			error = apiError;
			return;
		}

		handleClose();
	}
</script>

{#if action}
	{#key action.id}
		<AppDialog bind:open title={`record_action.${action.id.replaceAll(':', '.')}.name`} onclose={handleClose}>
			{#snippet description()}
				{recordActionDescription(action.id)}
			{/snippet}
			{#snippet body()}
				<form id={formId} class="flex flex-col gap-4" onsubmit={handleSubmit} novalidate>
					<SchemaForm schema={action.inputSchema} bind:values {error} {submitting} idPrefix="action-{action.id}">
						{#snippet after()}
							<label class="flex flex-col gap-1">
								<span class="text-label">{t('web.common.audit_comment')}</span>
								<textarea
									bind:value={auditComment}
									required={action.requiresAuditComment}
									disabled={submitting}
									rows={3}
									class="input w-full"></textarea>
							</label>
						{/snippet}
					</SchemaForm>
				</form>
			{/snippet}
			{#snippet footer()}
				<DialogActions {formId} confirmLabel={`record_action.${action.id.replaceAll(':', '.')}.name`} {submitting} />
			{/snippet}
		</AppDialog>
	{/key}
{/if}
