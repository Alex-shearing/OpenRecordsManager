<script lang="ts">
	import TrashIcon from 'phosphor-svelte/lib/TrashIcon';
	import { t } from '#lib/i18n/catalog.js';

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
	<span {id} class="sr-only">{t('web.config.list_values')}</span>
	{#each items as item, index (index)}
		<div class="flex items-center gap-2">
			<input
				id={index === 0 ? `${id}-item` : `${id}-item-${index}`}
				type="text"
				class="input min-w-0 flex-1 font-mono text-sm"
				{disabled}
				value={item}
				placeholder={t('web.config.value_placeholder')}
				aria-label="{t('web.config.value_placeholder')} {index + 1}"
				oninput={event => updateItem(index, event.currentTarget.value)}
			/>
			<button
				type="button"
				class="btn-ghost size-10 shrink-0 p-0!"
				{disabled}
				aria-label={t('web.config.remove_value')}
				onclick={() => removeItem(index)}
			>
				<TrashIcon class="size-4" aria-hidden="true" />
			</button>
		</div>
	{/each}
	<button
		type="button"
		class="btn-secondary self-start px-3"
		{disabled}
		aria-label={t('web.config.add_value')}
		onclick={addItem}
	>
		+
	</button>
</div>
