<script lang="ts">
	import { NavigationMenu } from 'bits-ui';
	import SearchBar from '$lib/components/SearchBar.svelte';

	let { children, data } = $props();
</script>

<NavigationMenu.Root
	class="relative z-15 flex w-full items-center justify-between gap-4 bg-pink-300 p-2 shadow"
>
	<!-- Left Side: Logo (Hidden on mobile) -->
	<div class="hidden items-center sm:flex">
		<a href="/" class="text-xl font-bold">
			{#if data.branding.logoUrl}
				<img src={data.branding.logoUrl} alt={data.branding.productName} class="h-8" />
			{:else}
				{data.branding.productName}
			{/if}
		</a>
	</div>

	<!-- Center/Main: SearchBar -->
	<!-- w-full makes it full width on mobile. sm:max-w-[333px] or sm:w-1/3 limits it on desktop -->
	<SearchBar class="w-full sm:w-1/2" />

	<!-- Right Side: Sign In (Hidden on mobile) -->
	<a
		href="/logout"
		class="hidden h-8 w-max items-center justify-center rounded-[7px] bg-transparent px-4 py-2 text-sm font-medium transition-colors hover:bg-white hover:text-accent-foreground focus:bg-muted focus:text-accent-foreground focus:outline-hidden disabled:pointer-events-none disabled:opacity-50 data-[state=open]:bg-white data-[state=open]:shadow-mini dark:hover:bg-muted dark:data-[state=open]:bg-muted group sm:inline-flex"
		data-sveltekit-preload-code="off"
		data-sveltekit-preload-data="off"
	>
		{data.me.username}
	</a>
</NavigationMenu.Root>

{@render children()}
