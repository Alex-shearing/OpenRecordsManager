<script lang="ts">
	let {
		auditComment = $bindable(''),
		required = false,
		requiredHint,
		optionalHint = 'Optional; recorded with each change.',
		formError = '',
		successMessage = '',
		submitting = false,
		dirty = false,
		onreset,
		class: className = '',
	}: {
		auditComment?: string;
		required?: boolean;
		requiredHint: string;
		optionalHint?: string;
		formError?: string;
		successMessage?: string;
		submitting?: boolean;
		dirty?: boolean;
		onreset?: () => void;
		class?: string;
	} = $props();
</script>

<section class="card p-5 {className}">
	<label class="flex flex-col gap-1">
		<span class="text-label">Audit comment</span>
		<span class="text-hint">{required ? requiredHint : optionalHint}</span>
		<textarea
			bind:value={auditComment}
			{required}
			disabled={submitting}
			rows={3}
			class="input w-full"
		></textarea>
	</label>

	{#if formError}
		<p class="mt-3 text-sm text-destructive" role="alert">{formError}</p>
	{/if}
	{#if successMessage}
		<p class="mt-3 text-sm text-foreground">{successMessage}</p>
	{/if}

	<div class="mt-4 flex flex-wrap gap-2">
		<button type="submit" class="btn-primary" disabled={submitting || !dirty}>
			{submitting ? 'Saving…' : 'Save changes'}
		</button>
		<button type="button" class="btn-secondary" disabled={submitting || !dirty} onclick={onreset}>
			Reset
		</button>
	</div>
</section>
