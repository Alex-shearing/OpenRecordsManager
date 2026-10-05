<script lang="ts">
	import type { SimpleObjectPropertyResponse } from '#lib/api/types.gen.js';
	import { objectPropertyName } from '#lib/i18n/labels.js';
	import { formatObjectPropertyValue } from '#lib/properties/formatValue.js';
	import type { Snippet } from 'svelte';

	let {
		properties,
		definitions = [],
		order = [],
		title,
		before,
		class: className = 'mb-8',
	}: {
		properties: Record<string, unknown>;
		definitions?: SimpleObjectPropertyResponse[];
		order?: string[];
		title?: string;
		before?: Snippet;
		class?: string;
	} = $props();

	const definitionById = $derived(new Map(definitions.map(entry => [entry.id, entry])));

	const sortedEntries = $derived(
		Object.entries(properties).toSorted(([a], [b]) => {
			const ai = order.indexOf(a);
			const bi = order.indexOf(b);
			if (ai !== -1 || bi !== -1) {
				if (ai === -1) return 1;
				if (bi === -1) return -1;
				return ai - bi;
			}
			return objectPropertyName(a).localeCompare(objectPropertyName(b));
		})
	);
</script>

<section class={['card p-4', className]}>
	{#if title}
		<h2 class="text-lg font-medium">{title}</h2>
	{/if}
	<dl class="mt-4 grid gap-3 sm:grid-cols-2">
		{@render before?.()}
		{#each sortedEntries as [key, value] (key)}
			<div>
				<dt class="text-hint">{objectPropertyName(key)}</dt>
				<dd>{formatObjectPropertyValue(definitionById.get(key), value)}</dd>
			</div>
		{/each}
	</dl>
</section>
