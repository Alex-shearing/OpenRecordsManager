<script lang="ts">
	import TrashIcon from 'phosphor-svelte/lib/TrashIcon';

	let {
		id,
		value = $bindable<string[]>(),
		disabled = false,
	}: {
		id: string;
		value: string[];
		disabled?: boolean;
	} = $props();

	let items = $state<string[]>(['']);

	$effect(() => {
		const normalized = value.length > 0 ? value : [''];
		if (JSON.stringify(items) !== JSON.stringify(normalized)) {
			items = normalized;
		}
	});

	function syncItems(nextItems: string[]) {
		items = nextItems.length > 0 ? nextItems : [''];
		value = items;
	}

	function updateItem(index: number, nextValue: string) {
		syncItems(items.map((item, itemIndex) => (itemIndex === index ? nextValue : item)));
	}

	function addItem() {
		syncItems([...items, '']);
	}

	function removeItem(index: number) {
		syncItems(items.filter((_, itemIndex) => itemIndex !== index));
	}
</script>

<div class="flex flex-col gap-2" role="group" aria-labelledby={id}>
	<span {id} class="sr-only">List values</span>
	{#each items as item, index (index)}
		<div class="flex items-center gap-2">
			<input
				id={index === 0 ? `${id}-item` : `${id}-item-${index}`}
				type="text"
				class="input min-w-0 flex-1 font-mono text-sm"
				{disabled}
				value={item}
				placeholder="Value"
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
