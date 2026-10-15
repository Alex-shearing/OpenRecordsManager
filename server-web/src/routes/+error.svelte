<script lang="ts">
	import { page } from '$app/state';
	import AppStatusView from '#lib/components/AppStatusView.svelte';
	import { t, tx } from '#lib/i18n/catalog.js';

	let title = $derived.by(() => {
		if (!page.data.online) {
			return tx('web.error.server_offline', 'Server offline');
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
			return tx(
				'web.error.server_offline_description',
				'The API could not be reached. Check that the server is running and try again.'
			);
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
	<title>{page.status} - {tx('web.layout.product_name', 'Open Records Manager')}</title>
</svelte:head>

<AppStatusView {title} {description} branding={page.data.branding} showSignIn={page.status === 401} />
