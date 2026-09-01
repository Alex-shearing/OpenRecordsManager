<script lang="ts">
	import TrashIcon from 'phosphor-svelte/lib/TrashIcon';

	let {
		id,
		value = $bindable<number[]>(),
		disabled = false,
	}: {
		id: string;
		value: number[];
		disabled?: boolean;
	} = $props();

	let items = $state<Array<number | null>>([]);

	$effect(() => {
		const normalized = value.length > 0 ? value.map(entry => entry as number | null) : [null];
		if (JSON.stringify(items) !== JSON.stringify(normalized)) {
			items = normalized;
		}
	});

	function syncValue(nextItems: Array<number | null>) {
		items = nextItems.length > 0 ? nextItems : [null];
		value = items.filter((item): item is number => item != null && !Number.isNaN(item));
	}

	function updateItem(index: number, raw: string) {
		const parsed = raw.trim() === '' ? null : Number.parseInt(raw, 10);
		const nextValue = parsed != null && Number.isNaN(parsed) ? null : parsed;
		syncValue(items.map((item, itemIndex) => (itemIndex === index ? nextValue : item)));
	}

	function addItem() {
		syncValue([...items, null]);
	}

	function removeItem(index: number) {
		syncValue(items.filter((_, itemIndex) => itemIndex !== index));
	}

	function displayValue(item: number | null): string {
		return item == null || Number.isNaN(item) ? '' : String(item);
	}
</script>

<div class="flex flex-col gap-2" role="group" aria-labelledby={id}>
	<span {id} class="sr-only">Integer list values</span>
	{#each items as item, index (index)}
		<div class="flex items-center gap-2">
			<input
				id={index === 0 ? `${id}-item` : `${id}-item-${index}`}
				type="number"
				step="1"
				class="input min-w-0 flex-1 font-mono text-sm"
				{disabled}
				value={displayValue(item)}
				placeholder="Integer value"
				aria-label="List item {index + 1}"
				oninput={event => updateItem(index, event.currentTarget.value)}
			/>
			<button
				type="button"
				class="btn-ghost size-10 shrink-0 p-0!"
				{disabled}
				aria-label="Remove item {index + 1}"
				onclick={() => removeItem(index)}
			>
				<TrashIcon class="size-4" aria-hidden="true" />
			</button>
		</div>
	{/each}
	<button type="button" class="btn-secondary self-start px-3" {disabled} aria-label="Add item" onclick={addItem}>
		+
	</button>
</div>
