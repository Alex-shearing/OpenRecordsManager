<script lang="ts">
	import { Select } from 'bits-ui';
	import CaretDownIcon from 'phosphor-svelte/lib/CaretDownIcon';
	import CaretUpIcon from 'phosphor-svelte/lib/CaretUpIcon';
	import CaretUpDownIcon from 'phosphor-svelte/lib/CaretUpDownIcon';
	import CheckIcon from 'phosphor-svelte/lib/CheckIcon';

	let { class: className, ...rest } = $props();

	const items = [
		{ label: 'All', value: 'all', search: 'Everything' },
		{ label: 'Record', value: 'record', search: 'Records' },
		{ label: 'Location', value: 'location', search: 'Locations' }
	];

	let selected = $state('all');
	let searchText = $derived(items.filter((a) => a.value == selected)[0].search);
</script>

<form class="flex items-center bg-white border-2 border-pink-950 rounded-2xl {className}">
	<Select.Root type="single" {items} bind:value={selected}>
		<Select.Trigger
			aria-label="Select a type"
			class="w-30 py-1.5 cursor-pointer inline-flex select-none items-center pl-2 text-sm font-medium text-zinc-800 hover:cursor-pointer"
		>
			<Select.Value placeholder="Type" />
			<CaretUpDownIcon class="text-muted-foreground ml-auto size-6" />
		</Select.Trigger>
		<Select.Portal>
			<Select.Content class="bg-red-600 w-30 z-20" sideOffset={3}>
				<Select.ScrollUpButton class="flex w-full items-center justify-center">
					<CaretUpIcon class="size-3" />
				</Select.ScrollUpButton>
				<Select.Viewport class="p-1">
					{#each items as item, i (i + item.value)}
						<Select.Item
							class="inline-flex justify-between w-full"
							value={item.value}
							label={item.label}
						>
							{#snippet children({ selected })}
								{item.label}
								{#if selected}
									<div class="ml-auto">
										<CheckIcon aria-label="check" />
									</div>
								{/if}
							{/snippet}
						</Select.Item>
					{/each}
				</Select.Viewport>
				<Select.ScrollDownButton class="flex w-full items-center justify-center">
					<CaretDownIcon class="size-3" />
				</Select.ScrollDownButton>
			</Select.Content>
		</Select.Portal>
	</Select.Root>

	<input
		id="text"
		placeholder="Search {searchText}..."
		class="placeholder:text-foreground-alt/50 w-full truncate text-base transition-colors sm:text-sm border-y-0 border-x-2"
	/>

	<button
		type="submit"
		class="shrink-0 px-3 py-1.5 text-sm font-medium text-zinc-800 hover:cursor-pointer"
	>
		Search
	</button>
</form>
