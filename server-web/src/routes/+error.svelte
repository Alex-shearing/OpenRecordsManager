<script lang="ts">
	import { page } from '$app/state';
	import BrandingHeader from '$lib/components/BrandingHeader.svelte';
	import PageContent from '$lib/components/layout/PageContent.svelte';

	let title = $derived.by(() => {
		if (!page.data.online) {
			return 'Server offline';
		}

		switch (page.status) {
			case 404:
				return 'Page not found';
			case 403:
				return 'Access denied';
			case 401:
				return 'Sign in required';
			default:
				return 'Something went wrong';
		}
	});

	let description = $derived.by(() => {
		if (!page.data.online) {
			return 'The server is currently offline, please try again later.';
		}

		switch (page.status) {
			case 404:
				return "The page you're looking for doesn't exist or may have been moved.";
			case 403:
				return "You don't have permission to view this page.";
			case 401:
				return 'You need to sign in to continue.';
			default:
				return page.error?.message || 'An unexpected error occurred. Please try again later.';
		}
	});
</script>

<svelte:head>
	<title>{page.status} · {page.data.branding.productName}</title>
</svelte:head>

<div class="flex h-dvh flex-col overflow-hidden">
	<BrandingHeader branding={page.data.branding} showLogoOnMobile />

	<div class="min-h-0 flex-1 overflow-y-auto">
		<PageContent variant="guest">
			<div class="card">
				<div class="card-header">
					<h1 class="text-2xl font-semibold">{title}</h1>
				</div>

				<div class="card-body">
					<p class="text-hint">{description}</p>

					<div class="mt-6 flex gap-3">
						<a href="/" class="btn-primary">Go home</a>
						{#if page.status === 401}
							<a href="/login" class="btn-secondary">Sign in</a>
						{/if}
					</div>
				</div>
			</div>

			{#if page.data.branding.supportUrl}
				<p class="mt-4 text-center text-hint">
					<a href={page.data.branding.supportUrl} class="text-link">Need help?</a>
				</p>
			{/if}
		</PageContent>
	</div>
</div>
