<script lang="ts">
	import { NavigationMenu } from 'bits-ui';
	import { page } from '$app/state';
	import type { Snippet } from 'svelte';

	let {
		href,
		match = 'prefix',
		children,
	}: {
		href: string;
		match?: 'exact' | 'prefix';
		children: Snippet;
	} = $props();

	const isActive = $derived(
		match === 'exact'
			? page.url.pathname === href
			: page.url.pathname === href || page.url.pathname.startsWith(`${href}/`)
	);
</script>

<NavigationMenu.Item>
	<NavigationMenu.Link
		{href}
		class="inline-flex items-center rounded-md px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-surface-hover hover:text-foreground aria-[current=page]:bg-primary/8 aria-[current=page]:text-primary"
		aria-current={isActive ? 'page' : undefined}
	>
		{@render children()}
	</NavigationMenu.Link>
</NavigationMenu.Item>
