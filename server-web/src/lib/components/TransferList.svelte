<script lang="ts" generics="T">
	import type { Snippet } from 'svelte';
	import { flip } from 'svelte/animate';
	import { MediaQuery } from 'svelte/reactivity';
	import { fade } from 'svelte/transition';
	import CaretDownIcon from 'phosphor-svelte/lib/CaretDownIcon';
	import CaretUpIcon from 'phosphor-svelte/lib/CaretUpIcon';
	import DotsSixVerticalIcon from 'phosphor-svelte/lib/DotsSixVerticalIcon';
	import MinusIcon from 'phosphor-svelte/lib/MinusIcon';
	import PlusIcon from 'phosphor-svelte/lib/PlusIcon';

	type ListSide = 'selected' | 'available';

	let {
		items,
		selected = $bindable<string[]>([]),
		getKey,
		disabled = false,
		labelledBy,
		selectedTitle = 'Selected',
		availableTitle = 'Available',
		selectedEmpty = 'None selected',
		availableEmpty = 'None available',
		selectedHint = 'Drag to reorder, or use the arrows.',
		availableHint = 'Drag or use + to include.',
		compareAvailable,
		item,
	}: {
		items: T[];
		selected?: string[];
		getKey: (item: T) => string;
		disabled?: boolean;
		labelledBy?: string;
		selectedTitle?: string;
		availableTitle?: string;
		selectedEmpty?: string;
		availableEmpty?: string;
		selectedHint?: string;
		availableHint?: string;
		compareAvailable?: (a: T, b: T) => number;
		item: Snippet<[T]>;
	} = $props();

	const baseId = $props.id();
	const selectedHeadingId = `${baseId}-selected`;
	const availableHeadingId = `${baseId}-available`;

	const byKey = $derived(new Map(items.map(entry => [getKey(entry), entry])));

	const selectedItems = $derived(selected.map(key => byKey.get(key)).filter((entry): entry is T => entry != null));

	const availableItems = $derived.by(() => {
		const selectedSet = new Set(selected);
		const remaining = items.filter(entry => !selectedSet.has(getKey(entry)));
		if (compareAvailable) {
			return [...remaining].sort(compareAvailable);
		}
		return remaining;
	});

	let dragKey = $state<string | null>(null);
	let dragFrom = $state<ListSide | null>(null);
	let dropSide = $state<ListSide | null>(null);
	let dropIndex = $state<number | null>(null);

	const reduceMotion = new MediaQuery('prefers-reduced-motion: reduce');
	const motionDuration = $derived(reduceMotion.current ? { flip: 0, fade: 0 } : { flip: 200, fade: 120 });

	function add(key: string) {
		if (disabled || selected.includes(key)) return;
		selected = [...selected, key];
	}

	function remove(key: string) {
		if (disabled) return;
		selected = selected.filter(id => id !== key);
	}

	function move(fromIndex: number, toIndex: number) {
		if (disabled || fromIndex === toIndex || fromIndex < 0 || toIndex < 0) return;
		if (fromIndex >= selected.length || toIndex >= selected.length) return;
		const next = [...selected];
		const [itemKey] = next.splice(fromIndex, 1);
		next.splice(toIndex, 0, itemKey);
		selected = next;
	}

	function clearDrag() {
		dragKey = null;
		dragFrom = null;
		dropSide = null;
		dropIndex = null;
	}

	function onDragStart(side: ListSide, key: string, event: DragEvent) {
		if (disabled) {
			event.preventDefault();
			return;
		}
		dragKey = key;
		dragFrom = side;
		event.dataTransfer?.setData('text/plain', key);
		if (event.dataTransfer) {
			event.dataTransfer.effectAllowed = side === 'selected' ? 'move' : 'copyMove';
		}
	}

	function onDragOverList(side: ListSide, event: DragEvent) {
		if (disabled || dragKey == null) return;
		event.preventDefault();
		dropSide = side;
		if (side === 'selected') {
			dropIndex = selected.length;
		} else {
			dropIndex = null;
		}
		if (event.dataTransfer) {
			event.dataTransfer.dropEffect = 'move';
		}
	}

	function onDragOverItem(side: ListSide, index: number, event: DragEvent) {
		if (disabled || dragKey == null) return;
		event.preventDefault();
		event.stopPropagation();
		dropSide = side;
		dropIndex = index;
		if (event.dataTransfer) {
			event.dataTransfer.dropEffect = 'move';
		}
	}

	function onDropList(side: ListSide, event: DragEvent) {
		event.preventDefault();
		if (disabled || dragKey == null || dragFrom == null) {
			clearDrag();
			return;
		}

		const key = dragKey;
		const from = dragFrom;
		const index = dropIndex;

		clearDrag();

		if (side === 'available') {
			if (from === 'selected') remove(key);
			return;
		}

		// Drop on selected list
		if (from === 'available') {
			if (selected.includes(key)) return;
			const insertAt = index ?? selected.length;
			const next = [...selected];
			next.splice(insertAt, 0, key);
			selected = next;
			return;
		}

		const fromIndex = selected.indexOf(key);
		if (fromIndex < 0) return;
		let toIndex = index ?? selected.length;
		if (toIndex > fromIndex) toIndex -= 1;
		move(fromIndex, toIndex);
	}

	function onDropItem(side: ListSide, index: number, event: DragEvent) {
		event.preventDefault();
		event.stopPropagation();
		dropSide = side;
		dropIndex = index;
		onDropList(side, event);
	}
</script>

<div
	class={['grid gap-3 sm:grid-cols-2', disabled && 'pointer-events-none opacity-60']}
	role="group"
	aria-labelledby={labelledBy}
>
	<section
		class={[
			'flex min-h-40 flex-col overflow-hidden rounded-lg border border-border bg-surface',
			dropSide === 'selected' && dragKey != null && 'ring-2 ring-primary/40',
		]}
		aria-labelledby={selectedHeadingId}
		ondragover={event => onDragOverList('selected', event)}
		ondrop={event => onDropList('selected', event)}
		ondragleave={() => {
			if (dropSide === 'selected') {
				dropSide = null;
				dropIndex = null;
			}
		}}
	>
		<header class="border-b border-border px-3 py-2">
			<h3 id={selectedHeadingId} class="text-label">{selectedTitle}</h3>
			<p class="text-hint text-xs">{selectedHint}</p>
		</header>

		{#if selectedItems.length === 0}
			<p class="p-3 text-sm text-hint">{selectedEmpty}</p>
		{:else}
			<ul class="flex flex-1 flex-col gap-1 p-2">
				{#each selectedItems as entry, index (getKey(entry))}
					<li
						class={[
							'flex items-center gap-1 rounded-md border border-transparent bg-surface px-1 py-1',
							dragKey === getKey(entry) && 'opacity-50',
							dropSide === 'selected' && dropIndex === index && 'border-primary',
						]}
						animate:flip={{ duration: motionDuration.flip }}
						in:fade={{ duration: motionDuration.fade }}
						out:fade={{ duration: motionDuration.fade }}
						draggable={!disabled}
						ondragstart={event => onDragStart('selected', getKey(entry), event)}
						ondragend={clearDrag}
						ondragover={event => onDragOverItem('selected', index, event)}
						ondrop={event => onDropItem('selected', index, event)}
					>
						<span class="shrink-0 text-muted-foreground" aria-hidden="true">
							<DotsSixVerticalIcon class="size-4" />
						</span>
						<div class="min-w-0 flex-1 text-sm">{@render item(entry)}</div>
						<div class="flex shrink-0 items-center">
							<button
								type="button"
								class="btn-ghost px-1.5 py-1"
								disabled={disabled || index === 0}
								aria-label="Move up"
								onclick={() => move(index, index - 1)}
							>
								<CaretUpIcon class="size-4" aria-hidden="true" />
							</button>
							<button
								type="button"
								class="btn-ghost px-1.5 py-1"
								disabled={disabled || index === selectedItems.length - 1}
								aria-label="Move down"
								onclick={() => move(index, index + 1)}
							>
								<CaretDownIcon class="size-4" aria-hidden="true" />
							</button>
							<button
								type="button"
								class="btn-ghost px-1.5 py-1"
								{disabled}
								aria-label="Remove"
								onclick={() => remove(getKey(entry))}
							>
								<MinusIcon class="size-4" aria-hidden="true" />
							</button>
						</div>
					</li>
				{/each}
			</ul>
		{/if}
	</section>

	<section
		class={[
			'flex min-h-40 flex-col overflow-hidden rounded-lg border border-border bg-surface',
			dropSide === 'available' && dragKey != null && 'ring-2 ring-primary/40',
		]}
		aria-labelledby={availableHeadingId}
		ondragover={event => onDragOverList('available', event)}
		ondrop={event => onDropList('available', event)}
		ondragleave={() => {
			if (dropSide === 'available') {
				dropSide = null;
				dropIndex = null;
			}
		}}
	>
		<header class="border-b border-border px-3 py-2">
			<h3 id={availableHeadingId} class="text-label">{availableTitle}</h3>
			<p class="text-hint text-xs">{availableHint}</p>
		</header>

		{#if availableItems.length === 0}
			<p class="p-3 text-sm text-hint">{availableEmpty}</p>
		{:else}
			<ul class="flex flex-1 flex-col gap-1 p-2">
				{#each availableItems as entry (getKey(entry))}
					<li
						class={[
							'flex items-center gap-1 rounded-md px-1 py-1',
							dragKey === getKey(entry) && 'opacity-50',
						]}
						animate:flip={{ duration: motionDuration.flip }}
						in:fade={{ duration: motionDuration.fade }}
						out:fade={{ duration: motionDuration.fade }}
						draggable={!disabled}
						ondragstart={event => onDragStart('available', getKey(entry), event)}
						ondragend={clearDrag}
					>
						<div class="min-w-0 flex-1 text-sm">{@render item(entry)}</div>
						<button
							type="button"
							class="btn-ghost shrink-0 px-1.5 py-1"
							{disabled}
							aria-label="Add"
							onclick={() => add(getKey(entry))}
						>
							<PlusIcon class="size-4" aria-hidden="true" />
						</button>
					</li>
				{/each}
			</ul>
		{/if}
	</section>
</div>
