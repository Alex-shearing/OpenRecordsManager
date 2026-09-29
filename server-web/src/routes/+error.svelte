<script lang="ts">
	import { page } from '$app/state';
	import BrandingHeader from '$lib/components/BrandingHeader.svelte';
	import PageContent from '$lib/components/layout/PageContent.svelte';
	import { t } from '$lib/i18n/catalog';

	let title = $derived.by(() => {
		if (!page.data.online) {
			return t('web.error.server_offline');
		}

		switch (page.status) {
			case 404:
				return t('web.error.not_found');
			case 403:
				return t('web.error.access_denied');
			case 401:
				return t('web.error.sign_in_required');
			default:
				return t('web.error.generic');
		}
	});

	let description = $derived.by(() => {
		if (!page.data.online) {
			return t('web.error.server_offline_description');
		}

		switch (page.status) {
			case 404:
				return t('web.error.not_found_description');
			case 403:
				return t('web.error.access_denied_description');
			case 401:
				return t('web.error.sign_in_required_description');
			default:
				return page.error?.message || t('web.error.generic_description');
		}
	});
</script>

<svelte:head>
	<title>{page.status} · {t('web.layout.product_name')}</title>
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
						<a href="/" class="btn-primary">{t('web.error.go_home')}</a>
						{#if page.status === 401}
							<a href="/login" class="btn-secondary">{t('web.error.sign_in')}</a>
						{/if}
					</div>
				</div>
			</div>

			{#if page.data.branding.supportUrl}
				<p class="mt-4 text-center text-hint">
					<a href={page.data.branding.supportUrl} class="text-link">{t('web.layout.need_help')}</a>
				</p>
			{/if}
		</PageContent>
	</div>
</div>
