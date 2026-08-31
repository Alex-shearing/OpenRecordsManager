<script lang="ts">
	import type { ConfigDraftValue, DescriminatedConfigTypeResponse } from '$lib/config/config-types';
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
		config: DescriminatedConfigTypeResponse;
		value: ConfigDraftValue;
		disabled?: boolean;
	} = $props();

	const inputId = $derived(`config-${config.key.replaceAll('.', '-')}`);
</script>

<ConfigSettingLayout {config} {inputId}>
	{#snippet input()}
		{#if config.type === 'boolean'}
			<ConfigBooleanInput id={inputId} bind:value={value as boolean} {disabled} />
		{:else if config.type === 'string_list'}
			<ConfigStringListInput id={inputId} bind:value={value as string[]} {disabled} />
		{:else if config.type === 'int_list'}
			<ConfigIntListInput id={inputId} bind:value={value as number[]} {disabled} />
		{:else if config.type === 'number'}
			<ConfigNumberInput id={inputId} bind:value={value as number | null} {disabled} />
		{:else if config.type === 'decimal'}
			<ConfigNumberInput id={inputId} bind:value={value as number | null} step="any" {disabled} />
		{:else if config.type === 'uuid'}
			<ConfigStringInput id={inputId} bind:value={value as string} {disabled} />
		{:else}
			<ConfigStringInput id={inputId} bind:value={value as string} {disabled} />
		{/if}
	{/snippet}
</ConfigSettingLayout>
