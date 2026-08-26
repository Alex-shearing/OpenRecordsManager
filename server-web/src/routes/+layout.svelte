<script lang="ts">
	import './layout.css';
	import faviconAsset from '$lib/assets/favicon.svg';
	import { config } from '$lib/config.svelte';

	let { children } = $props();

	let loadCfg = config.loadConfig();
	let cfg = config.getConfig();
</script>

<svelte:head>
	<title>{cfg.productName}</title>
	<link rel="icon" href={cfg.faviconUrl || faviconAsset} />
</svelte:head>

{#await loadCfg}
	<div class="flex min-h-screen items-center justify-center text-sm text-gray-500">Loading…</div>
{:then cfg} 
	<div style:--color-primary={cfg.primaryColor} style="display: contents">
		{@render children()}
	</div>
{/await}
