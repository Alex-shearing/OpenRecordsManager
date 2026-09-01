<script lang="ts">
	import type { Snippet } from 'svelte';

	let { children }: { children: Snippet } = $props();
</script>

<div class="mx-auto mb-5 w-full max-w-6xl px-4 py-6">
	{@render children()}
</div>
