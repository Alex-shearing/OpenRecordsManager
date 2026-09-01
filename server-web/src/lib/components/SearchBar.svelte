<script lang="ts">
	import { Select } from 'bits-ui';
	import CaretDownIcon from 'phosphor-svelte/lib/CaretDownIcon';
	import CaretUpIcon from 'phosphor-svelte/lib/CaretUpIcon';
	import CaretUpDownIcon from 'phosphor-svelte/lib/CaretUpDownIcon';
	import CheckIcon from 'phosphor-svelte/lib/CheckIcon';
	import MagnifyingGlassIcon from 'phosphor-svelte/lib/MagnifyingGlassIcon';

	let { class: className, ...rest } = $props();

	const items = [
		{ label: 'All', value: 'all', search: 'everything' },
		{ label: 'Record', value: 'record', search: 'records' },
		{ label: 'Location', value: 'location', search: 'locations' }
	];

	let selected = $state('all');
	let searchText = $derived(items.find((item) => item.value === selected)?.search ?? 'everything');

	function handleSubmit(event: SubmitEvent) {
		event.preventDefault();
	}
</script>

<form
	class="card flex items-stretch overflow-hidden p-0 {className}"
	onsubmit={handleSubmit}
	{...rest}
>
	<Select.Root type="single" {items} bind:value={selected}>
		<Select.Trigger
			aria-label="Select a search type"
			class="inline-flex w-28 shrink-0 items-center gap-1 border-r border-border px-3 py-2 text-sm font-medium text-foreground outline-none hover:bg-surface-hover"
		>
			<Select.Value placeholder="Type" />
			<CaretUpDownIcon class="ml-auto size-4 text-muted-foreground" />
		</Select.Trigger>
		<Select.Portal>
			<Select.Content
				class="z-50 min-w-28 rounded-lg border border-border bg-surface p-1 shadow-lg"
				sideOffset={4}
			>
				<Select.ScrollUpButton
					class="flex w-full items-center justify-center py-1 text-muted-foreground"
				>
					<CaretUpIcon class="size-3" />
				</Select.ScrollUpButton>
				<Select.Viewport>
					{#each items as item (item.value)}
						<Select.Item
							class="flex w-full items-center justify-between rounded px-2 py-1.5 text-sm text-foreground outline-none data-[highlighted]:bg-surface-muted"
							value={item.value}
							label={item.label}
						>
							{#snippet children({ selected })}
								{item.label}
								{#if selected}
									<CheckIcon class="size-4 text-primary" aria-label="Selected" />
								{/if}
							{/snippet}
						</Select.Item>
					{/each}
				</Select.Viewport>
				<Select.ScrollDownButton
					class="flex w-full items-center justify-center py-1 text-muted-foreground"
				>
					<CaretDownIcon class="size-3" />
				</Select.ScrollDownButton>
			</Select.Content>
		</Select.Portal>
	</Select.Root>

	<label class="flex min-w-0 flex-1 items-center gap-2 px-3">
		<MagnifyingGlassIcon class="size-4 shrink-0 text-subtle-foreground" aria-hidden="true" />
		<input
			type="search"
			placeholder="Search {searchText}..."
			class="min-w-0 flex-1 border-0 bg-transparent py-2 text-sm text-foreground placeholder:text-subtle-foreground focus:outline-none"
		/>
	</label>

	<button type="submit" class="btn-primary shrink-0 rounded-none border-l border-border">
		Search
	</button>
</form>
