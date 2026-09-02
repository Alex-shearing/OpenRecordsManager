<script lang="ts">
	import { fly } from 'svelte/transition';
	import CircleNotchIcon from 'phosphor-svelte/lib/CircleNotchIcon';

	let {
		form,
		auditComment = $bindable(''),
		required = false,
		requiredHint,
		optionalHint = 'Optional; recorded with each change.',
		formError = '',
		submitting = false,
		dirty = false,
		onreset,
	}: {
		form: string;
		auditComment?: string;
		required?: boolean;
		requiredHint: string;
		optionalHint?: string;
		formError?: string;
		submitting?: boolean;
		dirty?: boolean;
		onreset?: () => void;
	} = $props();

	const visible = $derived(dirty || submitting);

	let wasVisible = false;

	$effect(() => {
		const isVisible = dirty || submitting;
		if (wasVisible && !isVisible) {
			auditComment = '';
		}
		wasVisible = isVisible;
	});

	function setCommentValidity(event: Event) {
		const field = event.currentTarget as HTMLTextAreaElement;
		if (required && !field.value.trim()) {
			field.setCustomValidity(requiredHint);
			return;
		}
		field.setCustomValidity('');
	}
</script>

<div
	class="transition-[min-height] duration-300 ease-out"
	style:min-height={visible ? '6.5rem' : '0'}
	aria-hidden="true"
></div>

{#if visible}
	<div
		class="fixed inset-x-0 bottom-0 z-50 border-t border-border bg-surface shadow-[0_-8px_24px_-4px_rgb(0_0_0/0.12)]"
		role="region"
		aria-label="Unsaved changes"
		aria-busy={submitting}
		transition:fly={{ y: 96, duration: 280 }}
	>
		<div
			class="mx-auto flex w-full max-w-6xl flex-col gap-3 px-4 py-4 sm:flex-row sm:items-end sm:justify-between"
			class:opacity-60={submitting}
			class:pointer-events-none={submitting}
		>
			<div class="audit-comment-field min-w-0 flex-1">
				<label for="{form}-audit-comment" class="text-label">Audit comment</label>
				<p id="{form}-audit-comment-hint" class="audit-comment-hint mb-1 text-xs text-hint">
					{required ? requiredHint : optionalHint}
				</p>
				<textarea
					id="{form}-audit-comment"
					{form}
					name="audit-comment"
					bind:value={auditComment}
					{required}
					disabled={submitting}
					rows={2}
					aria-describedby="{form}-audit-comment-hint"
					class="input w-full resize-none"
					oninvalid={setCommentValidity}
					oninput={setCommentValidity}
				></textarea>

				{#if formError}
					<p class="mt-1.5 text-sm text-destructive" role="alert">{formError}</p>
				{/if}
			</div>

			<div class="flex shrink-0 flex-wrap items-center gap-2">
				<button type="submit" {form} class="btn-primary gap-2" disabled={submitting || !dirty}>
					{#if submitting}
						<CircleNotchIcon class="size-4 animate-spin" aria-hidden="true" />
						Saving…
					{:else}
						Save changes
					{/if}
				</button>
				<button type="button" class="btn-secondary" disabled={submitting || !dirty} onclick={() => onreset?.()}>
					Cancel
				</button>
			</div>
		</div>
	</div>
{/if}

<style>
	@reference "../../routes/layout.css";

	.audit-comment-field:has(textarea:user-invalid) .audit-comment-hint {
		@apply font-medium text-destructive;
	}
</style>
