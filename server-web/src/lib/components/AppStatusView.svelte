<script lang="ts">
	import type { WebBrandingResponse } from '#lib/api/types.gen.js';
	import BrandingHeader from '#lib/components/BrandingHeader.svelte';
	import PageContent from '#lib/components/layout/PageContent.svelte';
	import { tx } from '#lib/i18n/catalog.js';

	let {
		title,
		description,
		branding,
		showSignIn = false,
	}: {
		title: string;
		description: string;
		branding: WebBrandingResponse;
		showSignIn?: boolean;
	} = $props();
</script>

<div class="flex h-dvh flex-col overflow-hidden">
	<BrandingHeader {branding} showLogoOnMobile />

	<div class="min-h-0 flex-1 overflow-y-auto">
		<PageContent variant="guest">
			<div class="card">
				<div class="card-header">
					<h1 class="text-2xl font-semibold">{title}</h1>
				</div>

				<div class="card-body">
					<p class="text-hint">{description}</p>

					<div class="mt-6 flex gap-3">
						<a href="/" class="btn-primary">{tx('web.error.go_home', 'Go home')}</a>
						{#if showSignIn}
							<a href="/login" class="btn-secondary">{tx('web.error.sign_in', 'Sign in')}</a>
						{/if}
					</div>
				</div>
			</div>

			{#if branding.supportUrl}
				<p class="mt-4 text-center text-hint">
					<a href={branding.supportUrl} class="text-link">{tx('web.layout.need_help', 'Need help?')}</a>
				</p>
			{/if}
		</PageContent>
	</div>
</div>
