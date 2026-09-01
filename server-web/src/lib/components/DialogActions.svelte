<script lang="ts">
	let {
		confirmLabel,
		confirmingLabel = 'Working…',
		cancelLabel = 'Cancel',
		submitting = false,
		disabled = false,
		variant = 'primary',
		formId,
		onconfirm,
	}: {
		confirmLabel: string;
		confirmingLabel?: string;
		cancelLabel?: string;
		submitting?: boolean;
		disabled?: boolean;
		variant?: 'primary' | 'destructive';
		formId?: string;
		onconfirm?: () => void | Promise<void>;
	} = $props();

	const confirmClass = $derived(variant === 'destructive' ? 'btn-destructive' : 'btn-primary');
</script>

<div class="flex flex-wrap justify-end gap-2">
	<form method="dialog">
		<button type="submit" class="btn-secondary" disabled={submitting}>{cancelLabel}</button>
	</form>
	{#if formId}
		<button type="submit" form={formId} class={confirmClass} disabled={submitting || disabled}>
			{submitting ? confirmingLabel : confirmLabel}
		</button>
	{:else}
		<button type="button" class={confirmClass} disabled={submitting || disabled} onclick={onconfirm}>
			{submitting ? confirmingLabel : confirmLabel}
		</button>
	{/if}
</div>
