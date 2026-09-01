<script lang="ts">
	import { page } from '$app/state';
	import { NavigationMenu } from 'bits-ui';
	import BrandingHeader from '$lib/components/BrandingHeader.svelte';
	import HeaderNav from '$lib/components/HeaderNav.svelte';
	import HeaderNavLink from '$lib/components/layout/HeaderNavLink.svelte';
	import SearchBar from '$lib/components/SearchBar.svelte';
	import UserIcon from 'phosphor-svelte/lib/UserIcon';

	let { children, data } = $props();

	const isProfileActive = $derived(page.route.id === '/(authenticated)/profile');
</script>

<BrandingHeader branding={data.branding}>
	{#snippet center()}
		<div class="flex items-center gap-4">
			<SearchBar class="w-full sm:w-1/2" />
			<HeaderNav />
		</div>
	{/snippet}
	{#snippet end()}
		<NavigationMenu.Root aria-label="Account" class="shrink-0">
			<NavigationMenu.List class="flex list-none items-center gap-1">
				<HeaderNavLink
					route="/(authenticated)/profile"
					icon={UserIcon}
					active={isProfileActive}
					class="hidden sm:inline-flex"
				>
					{data.me.username}
				</HeaderNavLink>
			</NavigationMenu.List>
		</NavigationMenu.Root>
	{/snippet}
</BrandingHeader>

{@render children()}
