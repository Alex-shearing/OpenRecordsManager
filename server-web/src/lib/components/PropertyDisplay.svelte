<script lang="ts">
	import type { SimpleObjectPropertyResponse } from '#lib/api/types.gen.js';
	import { listElementName, objectPropertyName } from '#lib/i18n/labels.js';
	import type { Snippet } from 'svelte';

	let {
		properties,
		definitions = [],
		order = [],
		before,
	}: {
		properties: Record<string, unknown>;
		definitions?: SimpleObjectPropertyResponse[];
		order?: string[];
		before?: Snippet;
	} = $props();

	const definitionById = $derived(new Map(definitions.map(entry => [entry.id, entry])));

	function formatValue(key: string, value: unknown): string {
		if (value == null) {
			return '';
		}

		const definition = definitionById.get(key);
		if (definition?.type === 'list_item' && typeof value === 'string') {
			return listElementName(value);
		}
		if (definition?.type === 'list_multiple' && Array.isArray(value)) {
			return value.map(item => (typeof item === 'string' ? listElementName(item) : formatValue(key, item))).join(', ');
		}

		if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
			return String(value);
		}
		if (Array.isArray(value)) {
			return value.map(item => formatValue(key, item)).join(', ');
		}
		return JSON.stringify(value);
	}

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

<dl class="mt-4 grid gap-3 sm:grid-cols-2">
	{@render before?.()}
	{#each sortedEntries as [key, value] (key)}
		<div>
			<dt class="text-hint">{objectPropertyName(key)}</dt>
			<dd>{formatValue(key, value)}</dd>
		</div>
	{/each}
</dl>
