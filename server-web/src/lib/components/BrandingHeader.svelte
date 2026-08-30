<script lang="ts">
	import type { WebBrandingResponse } from '$lib/api/types.gen';
	import type { Snippet } from 'svelte';

	let {
		branding,
		showLogoOnMobile = false,
		center,
		end
	}: {
		branding: WebBrandingResponse;
		showLogoOnMobile?: boolean;
		center?: Snippet;
		end?: Snippet;
	} = $props();
</script>

<header
	class="sticky top-0 z-15 flex w-full items-center gap-4 p-2 shadow {center || end
		? 'justify-between'
		: 'justify-center'}"
	style:background-color={branding.primaryColor}
>
	<div class="items-center {showLogoOnMobile ? 'flex' : 'hidden sm:flex'}">
		<a href="/" class="flex items-center gap-2 text-xl font-bold">
			{#if branding.logoUrl}
				<img src={branding.logoUrl} alt={branding.productName} class="h-8" />
			{:else}
				{branding.productName}
			{/if}
		</a>
	</div>

	{#if center}
		<div class="min-w-0 flex-1">
			{@render center()}
		</div>
	{/if}

	{#if end}
		<div class="shrink-0">
			{@render end()}
		</div>
	{/if}
</header>
