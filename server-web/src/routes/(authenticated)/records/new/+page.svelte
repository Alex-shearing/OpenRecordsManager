<script lang="ts">
	import { page } from '$app/state';
	import { goto } from '$app/navigation';
	import { getClient } from '$lib';
	import PropertyFields from '$lib/components/PropertyFields.svelte';
	import type { components } from '$lib/types/schema';

	type RecordTypeResponse = components['schemas']['RecordTypeResponse'];

	let typeIds = $state<string[]>([]);
	let selectedType = $state('');
	let typeDetail = $state<RecordTypeResponse | null>(null);
	let values = $state<Record<string, unknown>>({});
	let loading = $state(true);
	let submitting = $state(false);
	let error = $state('');

	const queryType = $derived(page.url.searchParams.get('type') ?? '');

	$effect(() => {
		getClient()
			.GET('/api/record_types')
			.then(({ data, error: err }) => {
				loading = false;
				if (err || !data?.data) {
					error = err?.error ?? 'Failed to load record types';
					return;
				}
				typeIds = data.data;
				const initial = queryType && typeIds.includes(queryType) ? queryType : typeIds[0] ?? '';
				if (initial) selectedType = initial;
			});
	});

	$effect(() => {
		const id = selectedType;
		if (!id) {
			typeDetail = null;
			return;
		}
		getClient()
			.GET('/api/record_types/{id}', { params: { path: { id } } })
			.then(({ data }) => {
				typeDetail = data?.data ?? null;
				const next: Record<string, unknown> = {};
				for (const prop of typeDetail?.properties ?? []) {
					if (prop.default !== undefined) {
						next[prop.property.id] = prop.default;
					} else if (prop.property.type === 'boolean') {
						next[prop.property.id] = false;
					} else {
						next[prop.property.id] = '';
					}
				}
				values = next;
			});
	});

	async function create(event: SubmitEvent) {
		event.preventDefault();
		if (!selectedType) return;
		submitting = true;
		error = '';

		const properties: Record<string, unknown> = {};
		for (const [key, val] of Object.entries(values)) {
			if (val === '' || val === undefined) continue;
			properties[key] = val;
		}

		const { data, error: err } = await getClient().POST('/api/records', {
			body: { type: selectedType, properties }
		});

		submitting = false;
		if (err || !data?.data) {
			error = err?.error ?? 'Failed to create record';
			return;
		}
		await goto(`/records/${data.data.id}`);
	}
</script>

<div class="flex flex-col gap-4">
	<h1 class="text-2xl font-semibold">Create record</h1>

	{#if loading}
		<p class="text-sm text-gray-500">Loading types…</p>
	{:else if typeIds.length === 0}
		<p class="text-sm text-gray-500">
			No record types available.
			<a href="/admin/plugins/templates" class="text-(--color-primary) underline">Register a template</a>
		</p>
	{:else}
		<form class="flex flex-col gap-4" onsubmit={create}>
			<label class="flex flex-col gap-1">
				<span class="text-sm font-medium">Record type</span>
				<select
					bind:value={selectedType}
					class="rounded border border-gray-300 px-3 py-2 dark:border-gray-600"
					disabled={submitting}
				>
					{#each typeIds as id (id)}
						<option value={id}>{id}</option>
					{/each}
				</select>
			</label>

			{#if typeDetail}
				<p class="text-sm text-gray-600">{typeDetail.description}</p>
				<PropertyFields properties={typeDetail.properties} bind:values />
			{/if}

			{#if error}
				<p class="text-sm text-red-600">{error}</p>
			{/if}

			<button
				type="submit"
				disabled={submitting || !selectedType}
				class="self-start rounded bg-(--color-primary) px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
			>
				{submitting ? 'Creating…' : 'Create record'}
			</button>
		</form>
	{/if}
</div>
