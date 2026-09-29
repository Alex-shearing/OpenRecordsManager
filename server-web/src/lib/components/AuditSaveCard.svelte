<script lang="ts">
	import { fly } from 'svelte/transition';
	import CircleNotchIcon from 'phosphor-svelte/lib/CircleNotchIcon';
	import { t, tApiErrorResponse } from '$lib/i18n/catalog';
	import { type SchemaFormError } from './SchemaForm.svelte';

	let {
		form,
		auditComment = $bindable(''),
		required = false,
		error = undefined,
		submitting = false,
		dirty = false,
		onreset,
	}: {
		form: string;
		auditComment?: string;
		required?: boolean;
		error?: SchemaFormError;
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
		aria-label={t('web.common.unsaved_changes')}
		aria-busy={submitting}
		transition:fly={{ y: 96, duration: 280 }}
	>
		<div
			class="mx-auto flex w-full max-w-6xl flex-col gap-3 px-4 py-4 sm:flex-row sm:items-end sm:justify-between"
			class:opacity-60={submitting}
			class:pointer-events-none={submitting}
		>
			<div class="audit-comment-field min-w-0 flex-1">
				<label for="{form}-audit-comment" class="text-label">{t('web.common.audit_comment')}</label>
				<p id="{form}-audit-comment-hint" class="audit-comment-hint mb-1 text-xs text-hint">
					{required ? t('web.common.audit_comment_required') : t('web.common.audit_comment_optional')}
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
					class="input w-full resize-none"></textarea>

				{#if error}
					<p class="mt-1.5 text-sm text-destructive" role="alert">{tApiErrorResponse(error)}</p>
				{/if}
			</div>

			<div class="flex shrink-0 flex-wrap items-center gap-2">
				<button type="submit" {form} class="btn-primary gap-2" disabled={submitting || !dirty}>
					{#if submitting}
						<CircleNotchIcon class="size-4 animate-spin" aria-hidden="true" />
						{t('web.common.saving')}
					{:else}
						{t('web.common.save_changes')}
					{/if}
				</button>
				<button type="button" class="btn-secondary" disabled={submitting || !dirty} onclick={() => onreset?.()}>
					{t('web.common.cancel')}
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
