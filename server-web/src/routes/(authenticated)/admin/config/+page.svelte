<script lang="ts">
	import { getClient } from '$lib';
	import { formatValue } from '$lib/api';

	type Tab = 'database' | 'server';

	let tab = $state<Tab>('database');
	let dbConfig = $state<Record<string, unknown>>({});
	let serverConfig = $state<Record<string, unknown>>({});
	let error = $state('');
	let loading = $state(true);
	let editingKey = $state<string | null>(null);
	let editValue = $state('');
	let saving = $state(false);
	let lookupKey = $state('');
	let lookupResult = $state<string | null>(null);

	async function load() {
		loading = true;
		error = '';
		const [db, server] = await Promise.all([
			getClient().GET('/api/config'),
			getClient().GET('/api/config/this_server')
		]);
		loading = false;
		if (db.error || !db.data?.data) {
			error = db.error?.error ?? 'Failed to load config';
			return;
		}
		dbConfig = db.data.data as Record<string, unknown>;
		serverConfig = (server.data?.data as Record<string, unknown>) ?? {};
	}

	$effect(() => {
		load();
	});

	function startEdit(key: string, val: unknown) {
		editingKey = key;
		editValue = val == null ? '' : typeof val === 'string' ? val : JSON.stringify(val);
	}

	async function saveEdit() {
		if (!editingKey) return;
		saving = true;
		const { error: err } = await getClient().PUT('/api/config/{id}', {
			params: { path: { id: editingKey } },
			body: editValue
		});
		saving = false;
		if (err) {
			alert(err.error ?? 'Save failed');
			return;
		}
		editingKey = null;
		await load();
	}

	async function lookupOne(event: SubmitEvent) {
		event.preventDefault();
		lookupResult = null;
		const key = lookupKey.trim();
		if (!key) return;
		const path =
			tab === 'database'
				? await getClient().GET('/api/config/{id}', { params: { path: { id: key } } })
				: await getClient().GET('/api/config/this_server/{id}', { params: { path: { id: key } } });
		if (path.error) {
			lookupResult = path.error.error ?? 'Not found';
			return;
		}
		const data = path.data?.data;
		lookupResult =
			data && typeof data === 'object' && 'value' in data
				? formatValue((data as { value: unknown }).value)
				: formatValue(data);
	}

	let entries = $derived(
		Object.entries(tab === 'database' ? dbConfig : serverConfig).sort(([a], [b]) =>
			a.localeCompare(b)
		)
	);
</script>

<div class="flex flex-col gap-4">
	<h1 class="text-2xl font-semibold">Configuration</h1>

	<div class="flex gap-2">
		<button
			type="button"
			class="rounded px-3 py-1.5 text-sm {tab === 'database'
				? 'bg-(--color-primary) text-white'
				: 'border border-gray-300'}"
			onclick={() => (tab = 'database')}
		>
			Database
		</button>
		<button
			type="button"
			class="rounded px-3 py-1.5 text-sm {tab === 'server'
				? 'bg-(--color-primary) text-white'
				: 'border border-gray-300'}"
			onclick={() => (tab = 'server')}
		>
			This server
		</button>
	</div>

	<form class="flex flex-wrap gap-2" onsubmit={lookupOne}>
		<input
			bind:value={lookupKey}
			placeholder="Lookup key"
			class="rounded border border-gray-300 px-3 py-2 text-sm dark:border-gray-600"
		/>
		<button type="submit" class="rounded border border-gray-300 px-3 py-2 text-sm">Get key</button>
		{#if lookupResult !== null}
			<span class="text-sm text-gray-600">{lookupResult}</span>
		{/if}
	</form>

	{#if loading}
		<p class="text-sm text-gray-500">Loading…</p>
	{:else if error}
		<p class="text-sm text-red-600">{error}</p>
	{:else if entries.length === 0}
		<p class="text-sm text-gray-500">No config values</p>
	{:else}
		<ul class="divide-y divide-gray-200 rounded border border-gray-200 text-sm dark:border-gray-700">
			{#each entries as [key, val] (key)}
				<li class="px-4 py-3">
					{#if tab === 'database' && editingKey === key}
						<div class="flex flex-col gap-2">
							<span class="font-mono text-xs">{key}</span>
							<textarea
								bind:value={editValue}
								rows="2"
								class="rounded border border-gray-300 px-3 py-2 dark:border-gray-600"
							></textarea>
							<div class="flex gap-2">
								<button
									type="button"
									class="text-(--color-primary) underline"
									disabled={saving}
									onclick={saveEdit}
								>
									Save
								</button>
								<button
									type="button"
									class="text-gray-500 underline"
									onclick={() => (editingKey = null)}
								>
									Cancel
								</button>
							</div>
						</div>
					{:else}
						<div class="flex flex-wrap items-start justify-between gap-2">
							<div>
								<div class="font-mono text-xs text-gray-500">{key}</div>
								<div class="break-all">{formatValue(val)}</div>
							</div>
							{#if tab === 'database'}
								<button
									type="button"
									class="text-(--color-primary) underline"
									onclick={() => startEdit(key, val)}
								>
									Edit
								</button>
							{/if}
						</div>
					{/if}
				</li>
			{/each}
		</ul>
	{/if}
</div>
