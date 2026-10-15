<script lang="ts">
	import './layout.css';
	import faviconAsset from '#lib/assets/favicon.ico';
	import { afterNavigate } from '$app/navigation';
	import { Toaster } from 'svelte-hot-french-toast';
	import AppStatusView from '#lib/components/AppStatusView.svelte';
	import { tx } from '#lib/i18n/catalog.js';

	let { children, data } = $props();

	afterNavigate(async () => {
		// SW updates fetch service-worker.js + /_app/env.js from the network; skip when offline.
		if (!('serviceWorker' in navigator) || !navigator.onLine) return;
		try {
			const registration = await navigator.serviceWorker.getRegistration();
			await registration?.update();
		} catch {
			// Offline / flaky network — keep the active worker.
		}
	});

	const toastOptions = {
		class: 'orm-toast',
		duration: 3500,
		success: {
			iconTheme: {
				primary: 'var(--color-primary)',
				secondary: 'var(--color-primary-foreground)',
			},
		},
		error: {
			iconTheme: {
				primary: 'var(--color-destructive)',
				secondary: 'var(--color-destructive-foreground)',
			},
		},
	};
</script>

<svelte:head>
	<title>{tx('web.layout.product_name', 'Open Records Manager')}</title>
	<meta
		name="description"
		content={tx(
			'web.layout.meta_description',
			'Open Records Manager helps organizations manage records and information.'
		)}
	/>
	<link rel="icon" href={data.branding.faviconUrl || faviconAsset} />
	<link rel="manifest" href="/manifest.webmanifest" />
	<meta name="theme-color" content={data.branding.primaryColor} />
	<meta name="apple-mobile-web-app-capable" content="yes" />
	<meta name="apple-mobile-web-app-title" content={tx('web.layout.product_name', 'Open Records Manager')} />
	<link rel="apple-touch-icon" href="/icons/icon-192.png" />
</svelte:head>

<div style:--color-primary={data.branding.primaryColor} style="display: contents">
	<Toaster position="bottom-end" gutter={10} {toastOptions} />
	{#if !data.online}
		<AppStatusView
			title={tx('web.error.server_offline', 'Server offline')}
			description={tx(
				'web.error.server_offline_description',
				'The API could not be reached. Check that the server is running and try again.'
			)}
			branding={data.branding}
		/>
	{:else}
		{@render children()}
	{/if}
</div>
