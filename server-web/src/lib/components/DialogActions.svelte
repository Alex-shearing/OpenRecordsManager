<script lang="ts">
	import { t } from '#lib/i18n/catalog.js';

	let {
		confirmLabel,
		confirmingLabel = 'web.common.working',
		submitting = false,
		disabled = false,
		variant = 'primary',
		formId,
		onconfirm,
	}: {
		confirmLabel: string;
		confirmingLabel?: string;
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
		<button type="submit" class="btn-secondary" disabled={submitting}>{t('web.common.cancel')}</button>
	</form>
	{#if formId}
		<button type="submit" form={formId} class={confirmClass} disabled={submitting || disabled}>
			{submitting ? t(confirmingLabel) : t(confirmLabel)}
		</button>
	{:else}
		<button type="button" class={confirmClass} disabled={submitting || disabled} onclick={onconfirm}>
			{submitting ? t(confirmingLabel) : t(confirmLabel)}
		</button>
	{/if}
</div>
