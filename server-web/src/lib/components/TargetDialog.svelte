<script lang="ts" generics="T">
	import type { Snippet } from 'svelte';
	import AppDialog from './AppDialog.svelte';

	let {
		target = $bindable(null as T | null),
		title,
		description,
		body,
		footer,
		onclose,
		id,
	}: {
		target?: T | null;
		title?: string;
		description?: Snippet<[T]>;
		body?: Snippet<[T]>;
		footer?: Snippet<[T]>;
		onclose?: () => void;
		id?: string;
	} = $props();

	function handleClose() {
		target = null;
		onclose?.();
	}
</script>

{#snippet desc()}
	{#if target}
		{@render description!(target)}
	{/if}
{/snippet}

{#snippet bod()}
	{#if target}
		{@render body!(target)}
	{/if}
{/snippet}

{#snippet foot()}
	{#if target}
		{@render footer!(target)}
	{/if}
{/snippet}

<AppDialog
	open={!!target}
	{title}
	{id}
	onclose={handleClose}
	description={description ? desc : undefined}
	body={body ? bod : undefined}
	footer={footer ? foot : undefined}
/>
