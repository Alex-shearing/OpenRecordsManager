<script lang="ts">
	import SearchBar from '$lib/components/SearchBar.svelte';
	import BrandingHeader from '$lib/components/BrandingHeader.svelte';

	let { children, data } = $props();
</script>

<BrandingHeader branding={data.branding}>
	{#snippet center()}
		<SearchBar class="w-full sm:w-1/2" />
	{/snippet}
	{#snippet end()}
		<a
			href="/profile"
			class="hidden h-8 w-max items-center justify-center rounded-[7px] bg-transparent px-4 py-2 text-sm font-medium text-primary-foreground transition-colors hover:bg-white/15 focus:outline-hidden sm:inline-flex"
		>
			{data.me.username}
		</a>
	{/snippet}
</BrandingHeader>

{@render children()}
