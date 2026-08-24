<script lang="ts">
	import './layout.css';
	import faviconAsset from '$lib/assets/favicon.svg';
	import { loadRuntimeConfig, defaultRuntimeConfig, type RuntimeConfig } from '$lib/runtime-config';

	let { children } = $props();

	let config = $state<RuntimeConfig>(defaultRuntimeConfig);
	let ready = $state(false);

	loadRuntimeConfig().then((loaded) => {
		config = loaded;
		ready = true;
	});
</script>

<svelte:head>
	<title>{config.branding.productName}</title>
	<link rel="icon" href={config.branding.faviconUrl || faviconAsset} />
</svelte:head>

{#if ready}
	<div style:--color-primary={config.branding.primaryColor} style="display: contents">
		{@render children()}
	</div>
{:else}
	<div class="flex min-h-screen items-center justify-center text-sm text-gray-500">Loading…</div>
{/if}
