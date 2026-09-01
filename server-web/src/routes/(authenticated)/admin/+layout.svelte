<script lang="ts">
	import { NavigationMenu } from 'bits-ui';
	import SubNavLayout from '$lib/components/layout/SubNavLayout.svelte';
	import SubNavList from '$lib/components/layout/SubNavList.svelte';
	import SubNavLink from '$lib/components/layout/SubNavLink.svelte';
	import SubNavMenu from '$lib/components/layout/SubNavMenu.svelte';
	import SubNavMenuLink from '$lib/components/layout/SubNavMenuLink.svelte';

	let { children } = $props();
</script>

<SubNavLayout>
	{#snippet subnav()}
		<NavigationMenu.Root class="relative w-full">
			<SubNavList>
				<SubNavLink href="/admin/config" match="exact">Configuration</SubNavLink>

				<SubNavMenu label="Manage Plugins" hrefPrefix="/admin/plugins">
					<SubNavMenuLink href="/admin/plugins">Plugins</SubNavMenuLink>
					<SubNavMenuLink href="/admin/plugins/templates">Templates</SubNavMenuLink>
				</SubNavMenu>
			</SubNavList>

			<div class="perspective-[2000px] absolute top-full left-0 z-20 flex w-full justify-start">
				<NavigationMenu.Viewport
					class="text-popover-foreground relative mt-1 h-(--bits-navigation-menu-viewport-height) w-full origin-[top_left] overflow-hidden rounded-lg border border-border bg-surface shadow-lg transition-[width,height] duration-200 data-[state=closed]:animate-scale-out data-[state=open]:animate-scale-in sm:w-(--bits-navigation-menu-viewport-width)"
				/>
			</div>
		</NavigationMenu.Root>
	{/snippet}

	{@render children()}
</SubNavLayout>
