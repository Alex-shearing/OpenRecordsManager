<script lang="ts">
	import './layout.css';
	import faviconAsset from '$lib/assets/favicon.svg';
	import { Toaster } from 'svelte-hot-french-toast';

	let { children, data } = $props();

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
	<link rel="icon" href={data.branding.faviconUrl || faviconAsset} />
</svelte:head>

<div style:--color-primary={data.branding.primaryColor} style="display: contents">
	<Toaster position="bottom-end" gutter={10} {toastOptions} />
	{@render children()}
</div>
