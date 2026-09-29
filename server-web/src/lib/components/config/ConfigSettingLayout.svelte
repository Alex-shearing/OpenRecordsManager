<script lang="ts">
	import type { Snippet } from 'svelte';
	import type { ConfigTypeResponse } from '$lib/api/types.gen';
	import { formatDefaultValueForDisplay } from '$lib/config/config-utils';
	import { t } from '$lib/i18n/catalog';
	import { configDescription, configName } from '$lib/i18n/labels';

	let {
		config,
		inputId,
		input,
	}: {
		config: ConfigTypeResponse;
		inputId: string;
		input: Snippet;
	} = $props();
</script>

<article class="border-b border-border px-5 py-4 last:border-b-0">
	<div class="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
		<div class="min-w-0 flex-1">
			<label for={inputId} class="text-label">{configName(config.key)}</label>
			<p class="mt-1 text-hint">{configDescription(config.key)}</p>
			<p class="mt-2 font-mono text-xs text-subtle-foreground">{config.key}</p>
			{#if config.defaultValue != null && config.defaultValue !== ''}
				<p class="mt-1 text-xs text-subtle-foreground">
					{t('web.common.default_value', formatDefaultValueForDisplay(config))}
				</p>
			{/if}
		</div>

		<div class="w-full lg:max-w-md">
			{@render input()}
		</div>
	</div>
</article>
