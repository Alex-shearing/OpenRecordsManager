<script lang="ts">
	import { getClient } from '$lib';
	import type { components } from '$lib/types/schema';

	type SimpleList = components['schemas']['SimpleListTypeResponse'];

	let lists = $state<SimpleList[]>([]);
	let error = $state('');
	let loading = $state(true);
	let newId = $state('');
	let newName = $state('');
	let creating = $state(false);
	let createError = $state('');

	async function load() {
		loading = true;
		const { data, error: err } = await getClient().GET('/api/lists');
		loading = false;
		if (err || !data?.data) {
			error = err?.error ?? 'Failed to load lists';
			lists = [];
			return;
		}
		error = '';
		lists = data.data;
	}

	$effect(() => {
		load();
	});

	async function create(event: SubmitEvent) {
		event.preventDefault();
		creating = true;
		createError = '';
		const { error: err } = await getClient().POST('/api/lists', {
			body: { id: newId.trim(), name: newName.trim() }
		});
		creating = false;
		if (err) {
			createError = err.error ?? 'Create failed';
			return;
		}
		newId = '';
		newName = '';
		await load();
	}

	async function remove(id: string) {
		if (!confirm(`Delete list “${id}”?`)) return;
		const { error: err } = await getClient().DELETE('/api/lists/{list}', {
			params: { path: { list: id } }
		});
		if (err) {
			alert(err.error ?? 'Delete failed');
			return;
		}
		await load();
	}
</script>

<div class="flex flex-col gap-6">
	<h1 class="text-2xl font-semibold">Lists</h1>

	<section class="rounded-lg border border-gray-200 p-4 dark:border-gray-700">
		<h2 class="mb-3 text-lg font-medium">Create list type</h2>
		<form class="flex flex-col gap-2 sm:flex-row sm:items-end" onsubmit={create}>
			<label class="flex flex-col gap-1 text-sm">
				<span>ID</span>
				<input
					bind:value={newId}
					required
					placeholder="plugin:list_id"
					class="rounded border border-gray-300 px-3 py-2 dark:border-gray-600"
				/>
			</label>
			<label class="flex flex-col gap-1 text-sm">
				<span>Name</span>
				<input
					bind:value={newName}
					required
					class="rounded border border-gray-300 px-3 py-2 dark:border-gray-600"
				/>
			</label>
			<button
				type="submit"
				disabled={creating}
				class="rounded bg-(--color-primary) px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
			>
				{creating ? 'Creating…' : 'Create'}
			</button>
		</form>
		{#if createError}
			<p class="mt-2 text-sm text-red-600">{createError}</p>
		{/if}
	</section>

	{#if loading}
		<p class="text-sm text-gray-500">Loading…</p>
	{:else if error}
		<p class="text-sm text-red-600">{error}</p>
	{:else if lists.length === 0}
		<p class="text-sm text-gray-500">No lists</p>
	{:else}
		<ul class="divide-y divide-gray-200 rounded border border-gray-200 dark:border-gray-700">
			{#each lists as list (list.id)}
				<li class="flex items-center justify-between gap-3 px-4 py-3 text-sm">
					<a
						href="/lists/{encodeURIComponent(list.id)}"
						class="font-medium text-(--color-primary) underline"
					>
						{list.name}
						<span class="font-mono text-xs text-gray-500">({list.id})</span>
					</a>
					<button type="button" class="text-red-600 underline" onclick={() => remove(list.id)}>
						Delete
					</button>
				</li>
			{/each}
		</ul>
	{/if}
</div>
