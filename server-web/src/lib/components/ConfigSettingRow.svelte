<script lang="ts">
	import type { ConfigDraftValue } from '$lib/config/config-utils';
	import type { ConfigTypeResponse } from '$lib/api/types.gen';
	import ConfigBooleanInput from '$lib/components/config/ConfigBooleanInput.svelte';
	import ConfigIntListInput from '$lib/components/config/ConfigIntListInput.svelte';
	import ConfigNumberInput from '$lib/components/config/ConfigNumberInput.svelte';
	import ConfigSettingLayout from '$lib/components/config/ConfigSettingLayout.svelte';
	import ConfigStringInput from '$lib/components/config/ConfigStringInput.svelte';
	import ConfigStringListInput from '$lib/components/config/ConfigStringListInput.svelte';

	let {
		config,
		value = $bindable<ConfigDraftValue>(),
		disabled = false
	}: {
		config: ConfigTypeResponse;
		value: ConfigDraftValue;
		disabled?: boolean;
	} = $props();

	const inputId = $derived(`config-${config.key.replaceAll('.', '-')}`);
</script>

<ConfigSettingLayout {config} {inputId}>
	{#snippet input()}
		{#if value.type === 'boolean'}
			<ConfigBooleanInput id={inputId} bind:value={value.currentValue} {disabled} />
		{:else if value.type === 'string_list'}
			<ConfigStringListInput id={inputId} bind:value={value.currentValue} {disabled} />
		{:else if value.type === 'int_list'}
			<ConfigIntListInput id={inputId} bind:value={value.currentValue} {disabled} />
		{:else if value.type === 'number'}
			<ConfigNumberInput id={inputId} bind:value={value.currentValue} {disabled} />
		{:else if value.type === 'decimal'}
			<ConfigNumberInput id={inputId} bind:value={value.currentValue} step="any" {disabled} />
		{:else if value.type === 'uuid' || value.type === 'string'}
			<ConfigStringInput id={inputId} bind:value={value.currentValue} {disabled} />
		{:else}
			<p class="input w-full font-mono text-sm">This configuration cannot be modified from here, please change the configuration file directly.</p>
		{/if}
	{/snippet}
</ConfigSettingLayout>
