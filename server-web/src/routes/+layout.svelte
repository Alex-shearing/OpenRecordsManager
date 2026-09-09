<script lang="ts">
	import './layout.css';
	import faviconAsset from '$lib/assets/favicon.ico';
	import { afterNavigate } from '$app/navigation';
	import { Toaster } from 'svelte-hot-french-toast';

	let { children, data } = $props();

	afterNavigate(async () => {
		if ('serviceWorker' in navigator) {
			const registration = await navigator.serviceWorker.getRegistration();
			await registration?.update();
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
	<title>{data.branding.productName}</title>
	<meta name="description" content="{data.branding.productName} helps organizations manage records and information." />
	<link rel="icon" href={data.branding.faviconUrl || faviconAsset} />
	<link rel="manifest" href="/manifest.webmanifest" />
	<meta name="theme-color" content={data.branding.primaryColor} />
	<meta name="apple-mobile-web-app-capable" content="yes" />
	<meta name="apple-mobile-web-app-title" content={data.branding.productName} />
	<link rel="apple-touch-icon" href="/icons/icon-192.png" />
</svelte:head>

<div style:--color-primary={data.branding.primaryColor} style="display: contents">
	<Toaster position="bottom-end" gutter={10} {toastOptions} />
	{@render children()}
</div>
