<script lang="ts">
	import { page } from '$app/state';
	import BrandingHeader from '$lib/components/BrandingHeader.svelte';
	import HeaderNav from '$lib/components/HeaderNav.svelte';
	import SearchBar from '$lib/components/SearchBar.svelte';

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
		<a
			href="/profile"
			class="header-nav-link hidden sm:inline-flex"
			aria-current={isProfileActive ? 'page' : undefined}
		>
			{data.me.username}
		</a>
	{/snippet}
</BrandingHeader>

{@render children()}
