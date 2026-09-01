<script lang="ts">
	import PageContent from '$lib/components/layout/PageContent.svelte';
	import type { Snippet } from 'svelte';

	let { subnav, children }: { subnav: Snippet; children: Snippet } = $props();
</script>

<nav class="sticky top-(--header-height) z-10 w-full border-b border-border bg-surface" aria-label="Section navigation">
	<div class="mx-auto flex w-full max-w-6xl items-center px-4 py-1">
		{@render subnav()}
	</div>
</nav>

<PageContent>
	{@render children()}
</PageContent>
