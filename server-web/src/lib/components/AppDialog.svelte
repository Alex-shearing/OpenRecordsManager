<script lang="ts">
	import type { Snippet } from 'svelte';

	let {
		open = $bindable(false),
		title,
		description,
		body,
		footer,
		onclose,
		id,
	}: {
		open?: boolean;
		title?: string;
		description?: Snippet;
		body?: Snippet;
		footer?: Snippet;
		onclose?: () => void;
		id?: string;
	} = $props();

	let dialog = $state<HTMLDialogElement>();

	$effect(() => {
		if (!dialog) {
			return;
		}

		if (open) {
			if (!dialog.open) {
				dialog.showModal();
			}
			return;
		}

		if (dialog.open) {
			dialog.close();
		}
	});

	function handleClose() {
		open = false;
		onclose?.();
	}
</script>

<dialog bind:this={dialog} {id} class="app-dialog" onclose={handleClose}>
	{#if title || description}
		<div class="card-header flex items-start justify-between gap-4">
			<div>
				{#if title}
					<h2 class="text-lg font-medium">{title}</h2>
				{/if}
				{#if description}
					<div class="mt-1 text-hint">
						{@render description()}
					</div>
				{/if}
			</div>
			<form method="dialog">
				<button type="submit" aria-label="Close" class="btn-ghost px-2 py-0.5 text-muted-foreground">
					×
				</button>
			</form>
		</div>
	{/if}

	{#if body}
		<div class="card-body">
			{@render body()}
		</div>
	{/if}

	{#if footer}
		<div class="card-body border-t border-border">
			{@render footer()}
		</div>
	{/if}
</dialog>

<style>
	.app-dialog {
		position: fixed;
		inset: 0;
		width: fit-content;
		height: fit-content;
		max-width: min(32rem, calc(100vw - 2rem));
		max-height: calc(100vh - 2rem);
		margin: auto;
		padding: 0;
		overflow: auto;
		border: 1px solid var(--color-border);
		border-radius: var(--radius-lg);
		background: var(--color-surface);
		color: var(--color-foreground);
		box-shadow: var(--shadow-dialog);
		opacity: 0;
		transform: scale(0.97);
		transition:
			opacity 160ms ease-out,
			transform 160ms ease-out,
			overlay 160ms allow-discrete,
			display 160ms allow-discrete;
	}

	.app-dialog[open] {
		opacity: 1;
		transform: scale(1);
	}

	@starting-style {
		.app-dialog[open] {
			opacity: 0;
			transform: scale(0.97);
		}
	}

	.app-dialog::backdrop {
		background: rgb(0 0 0 / 0.25);
		opacity: 0;
		transition:
			opacity 160ms ease-out,
			overlay 160ms allow-discrete,
			display 160ms allow-discrete;
	}

	.app-dialog[open]::backdrop {
		opacity: 1;
	}

	@starting-style {
		.app-dialog[open]::backdrop {
			opacity: 0;
		}
	}

	@media (prefers-reduced-motion: reduce) {
		.app-dialog,
		.app-dialog::backdrop {
			transition: none;
			transform: none;
		}
	}
</style>
