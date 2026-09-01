<script lang="ts">
	import { NavigationMenu } from 'bits-ui';
	import { page } from '$app/state';
	import type { Snippet } from 'svelte';

	let {
		href,
		children,
	}: {
		href: string;
		children: Snippet;
	} = $props();

	const isActive = $derived(page.url.pathname === href || page.url.pathname.startsWith(`${href}/`));
</script>

<NavigationMenu.Link
	{href}
	class="block rounded-md px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-surface-hover hover:text-foreground aria-[current=page]:bg-primary/8 aria-[current=page]:text-primary"
	aria-current={isActive ? 'page' : undefined}
>
	{@render children()}
</NavigationMenu.Link>
