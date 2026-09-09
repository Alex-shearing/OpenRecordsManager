<script lang="ts">
	import { afterNavigate } from '$app/navigation';
	import BrandingHeader from '$lib/components/BrandingHeader.svelte';
	import PageContent from '$lib/components/layout/PageContent.svelte';

	let { children, data } = $props();

	let contentEl: HTMLDivElement | undefined = $state();

	afterNavigate(({ type }) => {
		if (type !== 'popstate') {
			contentEl?.scrollTo(0, 0);
		}
	});
</script>

<div class="flex h-dvh flex-col overflow-hidden">
	<BrandingHeader branding={data.branding} showLogoOnMobile />

	<div bind:this={contentEl} class="min-h-0 flex-1 overflow-y-auto">
		<PageContent variant="guest">
			{@render children()}

			{#if data.branding.supportUrl}
				<p class="mt-4 text-center text-hint">
					<a href={data.branding.supportUrl} class="text-link">Need help?</a>
				</p>
			{/if}
		</PageContent>
	</div>
</div>
