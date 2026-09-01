<script lang="ts">
	import type { Snippet } from 'svelte';

	let {
		children,
		variant = 'app',
	}: {
		children: Snippet;
		variant?: 'app' | 'guest';
	} = $props();
</script>

<main class="mx-auto w-full px-4 py-8 mb-5 {variant === 'guest' ? 'max-w-md' : 'max-w-6xl'}">
	{@render children()}
</main>
