<script lang="ts">
	import { getClient } from '$lib';
	import { formatValue } from '$lib/api';
	import type { components } from '$lib/types/schema';

	type RecordTypeProperty = components['schemas']['RecordTypePropertyResponse'];
	type ListElement = components['schemas']['ListElementResponse'];

	let {
		properties,
		values = $bindable<Record<string, unknown>>({})
	}: {
		properties: RecordTypeProperty[];
		values?: Record<string, unknown>;
	} = $props();

	function typeName(prop: RecordTypeProperty): string {
		return prop.property.type ?? 'string';
	}

	function propId(prop: RecordTypeProperty): string {
		return prop.property.id;
	}

	async function searchList(listId: string, value: string): Promise<ListElement[]> {
		if (!listId || !value.trim()) return [];
		const { data } = await getClient().GET('/api/lists/{list}/search', {
			params: { path: { list: listId }, query: { value } }
		});
		return data?.data ?? [];
	}

	let listQueries = $state<Record<string, string>>({});
	let listResults = $state<Record<string, ListElement[]>>({});

	async function onListSearch(prop: RecordTypeProperty) {
		const id = propId(prop);
		const listType = prop.property.listType?.id;
		if (!listType) return;
		listResults[id] = await searchList(listType, listQueries[id] ?? '');
	}

	function asBool(id: string): boolean {
		return Boolean(values[id]);
	}

	function setBool(id: string, checked: boolean) {
		values[id] = checked;
	}
</script>

<div class="flex flex-col gap-4">
	{#each properties as prop (prop.property.id)}
		{@const id = propId(prop)}
		{@const t = typeName(prop)}
		<label class="flex flex-col gap-1">
			<span class="text-sm font-medium">{prop.property.name}</span>
			{#if prop.property.description}
				<span class="text-sm text-gray-500">{prop.property.description}</span>
			{/if}

			{#if t === 'boolean'}
				<input
					type="checkbox"
					checked={asBool(id)}
					onchange={(e) => setBool(id, (e.currentTarget as HTMLInputElement).checked)}
					class="rounded"
				/>
			{:else if t === 'number' || t === 'decimal'}
				<input
					type="number"
					step={t === 'decimal' ? 'any' : '1'}
					class="rounded border border-gray-300 px-3 py-2 dark:border-gray-600"
					value={values[id] != null ? String(values[id]) : ''}
					oninput={(e) => {
						const v = (e.currentTarget as HTMLInputElement).value;
						values[id] = v === '' ? undefined : t === 'decimal' ? Number(v) : Number.parseInt(v, 10);
					}}
				/>
			{:else if t === 'date'}
				<input
					type="datetime-local"
					class="rounded border border-gray-300 px-3 py-2 dark:border-gray-600"
					value={typeof values[id] === 'string' ? (values[id] as string).slice(0, 16) : ''}
					oninput={(e) => {
						const v = (e.currentTarget as HTMLInputElement).value;
						values[id] = v ? new Date(v).toISOString() : undefined;
					}}
				/>
			{:else if t === 'list_item' || t === 'list_multiple'}
				<div class="flex flex-col gap-2">
					<div class="flex gap-2">
						<input
							type="text"
							placeholder="Search list…"
							class="flex-1 rounded border border-gray-300 px-3 py-2 dark:border-gray-600"
							bind:value={listQueries[id]}
						/>
						<button
							type="button"
							class="rounded border border-gray-300 px-3 py-2 text-sm"
							onclick={() => onListSearch(prop)}
						>
							Search
						</button>
					</div>
					{#if listResults[id]?.length}
						<ul class="max-h-40 overflow-auto rounded border border-gray-200 text-sm">
							{#each listResults[id] as el (String(el.type))}
								<li>
									<button
										type="button"
										class="w-full px-3 py-1.5 text-left hover:bg-gray-50"
										onclick={() => {
											if (t === 'list_multiple') {
												const current = Array.isArray(values[id])
													? [...(values[id] as string[])]
													: [];
												const key = String(el.type);
												if (!current.includes(key)) current.push(key);
												values[id] = current;
											} else {
												values[id] = String(el.type);
											}
										}}
									>
										{el.name}
										<span class="text-gray-400">({el.type})</span>
									</button>
								</li>
							{/each}
						</ul>
					{/if}
					<p class="text-xs text-gray-500">Selected: {formatValue(values[id])}</p>
				</div>
			{:else if t === 'calculated'}
				<input
					type="text"
					disabled
					value="(calculated)"
					class="rounded border border-gray-200 bg-gray-50 px-3 py-2 text-gray-500"
				/>
			{:else}
				<input
					type="text"
					class="rounded border border-gray-300 px-3 py-2 dark:border-gray-600"
					value={values[id] != null ? String(values[id]) : ''}
					oninput={(e) => {
						values[id] = (e.currentTarget as HTMLInputElement).value;
					}}
				/>
			{/if}
		</label>
	{/each}
</div>
