<script lang="ts">
	import { NavigationMenu } from 'bits-ui';
	import CaretDownIcon from 'phosphor-svelte/lib/CaretDownIcon';
	import cn from 'clsx';
	import SearchBar from '$lib/components/SearchBar.svelte';

	let { children } = $props();
</script>

<NavigationMenu.Root
	class="relative z-15 flex w-full items-center justify-between gap-4 bg-pink-300 p-2 shadow"
>
	<!-- Left Side: Logo (Hidden on mobile) -->
	<div class="hidden items-center sm:flex">
		<a href="/" class="text-xl font-bold">Logo</a>
	</div>

	<!-- Center/Main: SearchBar -->
	<!-- w-full makes it full width on mobile. sm:max-w-[333px] or sm:w-1/3 limits it on desktop -->
	<SearchBar class="w-full sm:w-1/2" />

	<!-- Right Side: Sign In (Hidden on mobile) -->
	<a
		href="/login"
		class="hidden h-8 w-max items-center justify-center rounded-[7px] bg-transparent px-4 py-2 text-sm font-medium transition-colors hover:bg-white hover:text-accent-foreground focus:bg-muted focus:text-accent-foreground focus:outline-hidden disabled:pointer-events-none disabled:opacity-50 data-[state=open]:bg-white data-[state=open]:shadow-mini dark:hover:bg-muted dark:data-[state=open]:bg-muted group sm:inline-flex"
	>
		Sign In
	</a>
</NavigationMenu.Root>

{@render children()}

<style lang="less">
</style>
