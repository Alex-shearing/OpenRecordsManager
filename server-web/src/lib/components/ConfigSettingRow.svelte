<script lang="ts">
	import type { ConfigTypeResponse } from '$lib/api/types.gen';
	import { formatConfigValueForDisplay } from '$lib/config/config-utils';

	let {
		config,
		value = $bindable<string>(),
		disabled = false
	}: {
		config: ConfigTypeResponse;
		value: string;
		disabled?: boolean;
	} = $props();

	const inputId = $derived(`config-${config.key.replaceAll('.', '-')}`);
</script>

<article class="border-b border-border px-5 py-4 last:border-b-0">
	<div class="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
		<div class="min-w-0 flex-1">
			<label for={inputId} class="text-label">{config.name}</label>
			<p class="mt-1 text-hint">{config.description}</p>
			<p class="mt-2 font-mono text-xs text-subtle-foreground">{config.key}</p>
			{#if config.defaultValue != null && config.defaultValue !== ''}
				<p class="mt-1 text-xs text-subtle-foreground">
					Default: {formatConfigValueForDisplay(config.defaultValue, config.type)}
				</p>
			{/if}
		</div>

		<div class="w-full lg:max-w-md">
			{#if config.type === 'boolean'}
				<label class="flex items-center gap-2">
					<input
						id={inputId}
						type="checkbox"
						class="size-4 rounded border-border-input"
						checked={value === 'true'}
						{disabled}
						onchange={(event) => {
							value = event.currentTarget.checked ? 'true' : 'false';
						}}
					/>
					<span class="text-sm text-foreground">{value === 'true' ? 'Enabled' : 'Disabled'}</span>
				</label>
			{:else if config.type === 'string_list' || config.type === 'int_list'}
				<textarea
					id={inputId}
					bind:value
					{disabled}
					rows={4}
					class="input w-full font-mono text-sm"
					placeholder={config.type === 'int_list' ? 'One integer per line' : 'One value per line'}
				></textarea>
			{:else if config.type === 'number' || config.type === 'decimal'}
				<input
					id={inputId}
					type="number"
					step={config.type === 'decimal' ? 'any' : '1'}
					bind:value
					{disabled}
					class="input w-full"
				/>
			{:else}
				<input id={inputId} type="text" bind:value {disabled} class="input w-full" />
			{/if}
		</div>
	</div>
</article>
