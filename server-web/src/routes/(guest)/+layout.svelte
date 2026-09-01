<script lang="ts">
	import BrandingHeader from '$lib/components/BrandingHeader.svelte';
	import PageContent from '$lib/components/layout/PageContent.svelte';

	let { children, data } = $props();
</script>

<BrandingHeader branding={data.branding} showLogoOnMobile />

<PageContent variant="guest">
	{@render children()}

	{#if data.branding.supportUrl}
		<p class="mt-4 text-center text-hint">
			<a href={data.branding.supportUrl} class="text-link">Need help?</a>
		</p>
	{/if}
</PageContent>
