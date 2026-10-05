<script lang="ts">
	import type { Snippet } from 'svelte';

	let {
		children,
		variant = 'app',
	}: {
		children: Snippet;
		variant?: 'app' | 'guest' | 'wide';
	} = $props();

	const maxWidths = {
		app: 'max-w-6xl',
		guest: 'max-w-md',
		wide: 'max-w-screen-2xl',
	} as const;

	const maxWidth = $derived(maxWidths[variant]);
</script>

<main class="mx-auto mb-5 w-full px-4 py-8 {maxWidth}">
	{@render children()}
</main>
