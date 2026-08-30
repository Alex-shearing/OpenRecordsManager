<script lang="ts">
	import { getClient } from '$lib';
	import SchemaForm from '$lib/components/SchemaForm.svelte';
	import type { components } from '$lib/types/schema';

	type FileStore = components['schemas']['FileStoreResponse'] | components['schemas']['SimpleFileStoreResponse'];
	type Middleware =
		| components['schemas']['MiddlewareResponse']
		| components['schemas']['SimpleMiddlewareResponse'];
	type FileStoreType = components['schemas']['FileStoreTypeResponse'];
	type MiddlewareType = components['schemas']['MiddlewareTypeResponse'];
	type InputFormSchema = components['schemas']['InputFormSchema'];

	let stores = $state<FileStore[]>([]);
	let middlewares = $state<Middleware[]>([]);
	let storeTypes = $state<FileStoreType[]>([]);
	let middlewareTypes = $state<MiddlewareType[]>([]);
	let error = $state('');
	let loading = $state(true);

	let selectedStoreType = $state('');
	let storeValues = $state<Record<string, string>>({});
	let storeMiddlewareIds = $state('');
	let creatingStore = $state(false);
	let storeError = $state('');

	let selectedMwType = $state('');
	let mwValues = $state<Record<string, string>>({});
	let creatingMw = $state(false);
	let mwError = $state('');

	function emptyFromSchema(schema?: InputFormSchema) {
		return Object.fromEntries(Object.keys(schema?.properties ?? {}).map((k) => [k, '']));
	}

	function coerceSettings(schema: InputFormSchema | undefined, values: Record<string, string>) {
		const out: Record<string, unknown> = {};
		for (const [key, raw] of Object.entries(values)) {
			if (raw === '') continue;
			const field = schema?.properties?.[key];
			if (field?.type === 'number' || field?.type === 'integer') {
				out[key] = Number(raw);
			} else if (field?.type === 'boolean') {
				out[key] = raw === 'true' || raw === '1';
			} else {
				out[key] = raw;
			}
		}
		return out;
	}

	let activeStoreSchema = $derived(
		storeTypes.find((t) => t.id === selectedStoreType)?.settingsSchema
	);
	let activeMwSchema = $derived(
		middlewareTypes.find((t) => t.id === selectedMwType)?.settingsSchema
	);

	async function load() {
		loading = true;
		const [s, m, st, mt] = await Promise.all([
			getClient().GET('/api/file_stores'),
			getClient().GET('/api/file_stores/middlewares'),
			getClient().GET('/api/file_stores/types'),
			getClient().GET('/api/file_stores/middlewares/types')
		]);
		loading = false;
		if (s.error) {
			error = s.error.error ?? 'Failed to load';
			return;
		}
		error = '';
		stores = s.data?.data ?? [];
		middlewares = m.data?.data ?? [];
		storeTypes = st.data?.data ?? [];
		middlewareTypes = mt.data?.data ?? [];
		if (!selectedStoreType && storeTypes[0]) {
			selectedStoreType = storeTypes[0].id;
			storeValues = emptyFromSchema(storeTypes[0].settingsSchema);
		}
		if (!selectedMwType && middlewareTypes[0]) {
			selectedMwType = middlewareTypes[0].id;
			mwValues = emptyFromSchema(middlewareTypes[0].settingsSchema);
		}
	}

	$effect(() => {
		load();
	});

	function onStoreTypeChange(event: Event) {
		selectedStoreType = (event.currentTarget as HTMLSelectElement).value;
		storeValues = emptyFromSchema(activeStoreSchema);
	}

	function onMwTypeChange(event: Event) {
		selectedMwType = (event.currentTarget as HTMLSelectElement).value;
		mwValues = emptyFromSchema(activeMwSchema);
	}

	async function createStore() {
		creatingStore = true;
		storeError = '';
		const { error: err } = await getClient().POST('/api/file_stores', {
			body: {
				type: selectedStoreType,
				properties: coerceSettings(activeStoreSchema, storeValues),
				middlewares: storeMiddlewareIds
					.split(',')
					.map((id) => id.trim())
					.filter(Boolean)
			}
		});
		creatingStore = false;
		if (err) {
			storeError = err.error ?? 'Create failed';
			return;
		}
		storeMiddlewareIds = '';
		storeValues = emptyFromSchema(activeStoreSchema);
		await load();
	}

	async function createMiddleware() {
		creatingMw = true;
		mwError = '';
		const { error: err } = await getClient().POST('/api/file_stores/middlewares', {
			body: {
				type: selectedMwType,
				properties: coerceSettings(activeMwSchema, mwValues)
			}
		});
		creatingMw = false;
		if (err) {
			mwError = err.error ?? 'Create failed';
			return;
		}
		mwValues = emptyFromSchema(activeMwSchema);
		await load();
	}

	async function deleteStore(id: string) {
		if (!confirm('Delete this file store?')) return;
		const { error: err } = await getClient().DELETE('/api/file_stores/{id}', {
			params: { path: { id } }
		});
		if (err) alert(err.error ?? 'Delete failed');
		else await load();
	}

	async function deleteMiddleware(id: string) {
		if (!confirm('Delete this middleware?')) return;
		const { error: err } = await getClient().DELETE('/api/file_stores/middlewares/{id}', {
			params: { path: { id } }
		});
		if (err) alert(err.error ?? 'Delete failed');
		else await load();
	}

	async function refreshStore(id: string) {
		const { data, error: err } = await getClient().GET('/api/file_stores/{id}', {
			params: { path: { id } }
		});
		if (err || !data?.data) {
			alert(err?.error ?? 'Failed to load store');
			return;
		}
		const detail = data.data;
		const propsRaw = prompt('Properties JSON', JSON.stringify(detail.properties ?? {}, null, 2));
		if (propsRaw === null) return;
		let properties: Record<string, unknown>;
		try {
			properties = JSON.parse(propsRaw);
		} catch {
			alert('Invalid JSON');
			return;
		}
		const { error: putErr } = await getClient().PUT('/api/file_stores/{id}', {
			params: { path: { id } },
			body: properties
		});
		if (putErr) alert(putErr.error ?? 'Update failed');
		else await load();
	}

	async function refreshMiddleware(id: string) {
		const { data, error: err } = await getClient().GET('/api/file_stores/middlewares/{id}', {
			params: { path: { id } }
		});
		if (err || !data?.data) {
			alert(err?.error ?? 'Failed to load middleware');
			return;
		}
		const detail = data.data;
		const propsRaw = prompt('Properties JSON', JSON.stringify(detail.properties ?? {}, null, 2));
		if (propsRaw === null) return;
		let properties: Record<string, unknown>;
		try {
			properties = JSON.parse(propsRaw);
		} catch {
			alert('Invalid JSON');
			return;
		}
		const { error: putErr } = await getClient().PUT('/api/file_stores/middlewares/{id}', {
			params: { path: { id } },
			body: properties
		});
		if (putErr) alert(putErr.error ?? 'Update failed');
		else await load();
	}
</script>

<div class="flex flex-col gap-8">
	<h1 class="text-2xl font-semibold">File stores</h1>

	{#if loading}
		<p class="text-sm text-gray-500">Loading…</p>
	{:else if error}
		<p class="text-sm text-red-600">{error}</p>
	{:else}
		<section class="rounded-lg border border-gray-200 p-4 dark:border-gray-700">
			<h2 class="mb-3 text-lg font-medium">Stores</h2>
			{#if stores.length === 0}
				<p class="mb-4 text-sm text-gray-500">No file stores</p>
			{:else}
				<ul class="mb-4 divide-y divide-gray-200 rounded border border-gray-100 text-sm">
					{#each stores as store (store.id)}
						<li class="flex flex-wrap items-center justify-between gap-2 px-3 py-2">
							<span>
								<span class="font-mono text-xs">{store.id}</span>
								<span class="ml-2">{store.type}</span>
							</span>
							<span class="flex gap-2">
								<button
									type="button"
									class="text-(--color-primary) underline"
									onclick={() => refreshStore(store.id)}
								>
									Edit
								</button>
								<button
									type="button"
									class="text-red-600 underline"
									onclick={() => deleteStore(store.id)}
								>
									Delete
								</button>
							</span>
						</li>
					{/each}
				</ul>
			{/if}

			{#if storeTypes.length}
				<label class="mb-3 flex flex-col gap-1 text-sm">
					<span>Type</span>
					<select
						value={selectedStoreType}
						onchange={onStoreTypeChange}
						class="rounded border border-gray-300 px-3 py-2"
					>
						{#each storeTypes as t (t.id)}
							<option value={t.id}>{t.id}</option>
						{/each}
					</select>
				</label>
				{#if activeStoreSchema}
					<SchemaForm
						schema={activeStoreSchema}
						bind:values={storeValues}
						submitting={creatingStore}
						formError={storeError}
						submitLabel="Create store"
						onsubmit={createStore}
					/>
				{/if}
				<label class="mt-3 flex flex-col gap-1 text-sm">
					<span>Middleware IDs (comma-separated, optional)</span>
					<input
						bind:value={storeMiddlewareIds}
						class="rounded border border-gray-300 px-3 py-2"
						placeholder="uuid, uuid"
					/>
				</label>
			{:else}
				<p class="text-sm text-gray-500">No store types available from plugins</p>
			{/if}
		</section>

		<section class="rounded-lg border border-gray-200 p-4 dark:border-gray-700">
			<h2 class="mb-3 text-lg font-medium">Middlewares</h2>
			{#if middlewares.length === 0}
				<p class="mb-4 text-sm text-gray-500">No middlewares</p>
			{:else}
				<ul class="mb-4 divide-y divide-gray-200 rounded border border-gray-100 text-sm">
					{#each middlewares as mw (mw.id)}
						<li class="flex flex-wrap items-center justify-between gap-2 px-3 py-2">
							<span>
								<span class="font-mono text-xs">{mw.id}</span>
								<span class="ml-2">{mw.type}</span>
							</span>
							<span class="flex gap-2">
								<button
									type="button"
									class="text-(--color-primary) underline"
									onclick={() => refreshMiddleware(mw.id)}
								>
									Edit
								</button>
								<button
									type="button"
									class="text-red-600 underline"
									onclick={() => deleteMiddleware(mw.id)}
								>
									Delete
								</button>
							</span>
						</li>
					{/each}
				</ul>
			{/if}

			{#if middlewareTypes.length}
				<label class="mb-3 flex flex-col gap-1 text-sm">
					<span>Type</span>
					<select
						value={selectedMwType}
						onchange={onMwTypeChange}
						class="rounded border border-gray-300 px-3 py-2"
					>
						{#each middlewareTypes as t (t.id)}
							<option value={t.id}>{t.id}</option>
						{/each}
					</select>
				</label>
				{#if activeMwSchema}
					<SchemaForm
						schema={activeMwSchema}
						bind:values={mwValues}
						submitting={creatingMw}
						formError={mwError}
						submitLabel="Create middleware"
						onsubmit={createMiddleware}
					/>
				{/if}
			{:else}
				<p class="text-sm text-gray-500">No middleware types available from plugins</p>
			{/if}
		</section>
	{/if}
</div>
