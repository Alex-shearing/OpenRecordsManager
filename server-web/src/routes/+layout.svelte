<script lang="ts">
	import './layout.css';
	import faviconAsset from '$lib/assets/favicon.svg';

	let { children, data } = $props();
</script>

<svelte:head>
	<title>{data.branding.productName}</title>
	<link rel="icon" href={data.branding.faviconUrl || faviconAsset} />
</svelte:head>

<div style:--color-primary={data.branding.primaryColor} style="display: contents">
	{@render children()}
</div>
