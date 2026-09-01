<script lang="ts">
	import { NavigationMenu } from 'bits-ui';
	import { page } from '$app/state';
	import type { Snippet } from 'svelte';
	import type { RouteId } from '$app/types';
	import { resolve, resolveRoute } from '$app/paths';

	let {
		route,
		children,
	}: {
		route: RouteId;
		children: Snippet;
	} = $props();

	const isActive = $derived(page.route.id === route);
</script>

<NavigationMenu.Link
	href={resolve(route)}
	class="block rounded-md px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-surface-hover hover:text-foreground aria-[current=page]:bg-primary/8 aria-[current=page]:text-primary"
	aria-current={isActive ? 'page' : undefined}
>
	{@render children()}
</NavigationMenu.Link>
