<script lang="ts">
	import { afterNavigate } from '$app/navigation';
	import { page } from '$app/state';
	import BrandingHeader from '$lib/components/BrandingHeader.svelte';
	import HeaderNav from '$lib/components/HeaderNav.svelte';
	import HeaderNavLink from '$lib/components/layout/HeaderNavLink.svelte';
	import SearchBar from '$lib/components/SearchBar.svelte';
	import UserIcon from 'phosphor-svelte/lib/UserIcon';

	let { children, data } = $props();

	const isProfileActive = $derived(page.route.id === '/(authenticated)/profile');

	let contentEl: HTMLDivElement | undefined = $state();

	afterNavigate(({ type }) => {
		if (type !== 'popstate') {
			contentEl?.scrollTo(0, 0);
		}
	});
</script>

<div class="flex h-dvh flex-col overflow-hidden">
	<BrandingHeader branding={data.branding}>
		{#snippet center()}
			<div class="flex items-center gap-4">
				<SearchBar />
				<HeaderNav />
			</div>
		{/snippet}
		{#snippet end()}
			<nav aria-label="Account" class="flex shrink-0 items-center gap-1">
				<HeaderNavLink
					route="/(authenticated)/profile"
					icon={UserIcon}
					active={isProfileActive}
					class="hidden sm:inline-flex"
				>
					{data.me.username}
				</HeaderNavLink>
			</nav>
		{/snippet}
	</BrandingHeader>

	<div bind:this={contentEl} class="min-h-0 flex-1 overflow-y-auto">
		{@render children()}
	</div>
</div>
