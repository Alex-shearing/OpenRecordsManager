<script lang="ts">
	import type { Snippet } from 'svelte';
	import AdminFormError from '#lib/components/AdminFormError.svelte';
	import AuditCommentField from '#lib/components/AuditCommentField.svelte';
	import type { SchemaFormError } from '#lib/components/SchemaForm.svelte';
	import { t } from '#lib/i18n/catalog.js';

	let {
		title,
		hint,
		onsubmit,
		submitting = false,
		submitDisabled = false,
		error = undefined,
		auditComment = $bindable(''),
		auditRequired = false,
		children,
	}: {
		title: string;
		hint: string;
		onsubmit: (event: SubmitEvent) => void | Promise<void>;
		submitting?: boolean;
		submitDisabled?: boolean;
		error?: SchemaFormError;
		auditComment?: string;
		auditRequired?: boolean;
		children: Snippet;
	} = $props();
</script>

<form class="mt-6 card p-5" {onsubmit}>
	<h2 class="mb-1 text-lg font-medium">{title}</h2>
	<p class="mb-4 text-hint">{hint}</p>

	{@render children()}

	<AdminFormError {error} class="mb-4" />

	<AuditCommentField
		class="mb-4"
		bind:value={auditComment}
		required={auditRequired}
		disabled={submitting}
	/>

	<button type="submit" class="btn-primary" disabled={submitting || submitDisabled}>
		{submitting ? t('web.common.creating') : t('web.common.create')}
	</button>
</form>
