<script lang="ts">
	import { page } from '$app/state';
	import { getClient } from '$lib';
	import type { components } from '$lib/types/schema';

	type ListType = components['schemas']['ListTypeResponse'];
	type ListElement = components['schemas']['ListElementResponse'];

	let listId = $derived(decodeURIComponent(page.params.list ?? ''));
	let list = $state<ListType | null>(null);
	let error = $state('');
	let loading = $state(true);
	let nameEdit = $state('');
	let savingName = $state(false);

	let search = $state('');
	let searchResults = $state<ListElement[] | null>(null);

	let elId = $state('');
	let elName = $state('');
	let elDesc = $state('');
	let elIndex = $state('0');
	let elAliases = $state('');
	let elActiveTo = $state('');
	let creating = $state(false);
	let createError = $state('');

	let editing = $state<string | null>(null);
	let editName = $state('');
	let editDesc = $state('');
	let editIndex = $state('0');
	let editAliases = $state('');
	let editActiveTo = $state('');

	async function load() {
		loading = true;
		const { data, error: err } = await getClient().GET('/api/lists/{list}', {
			params: { path: { list: listId } }
		});
		loading = false;
		if (err || !data?.data) {
			error = err?.error ?? 'Failed to load list';
			list = null;
			return;
		}
		error = '';
		list = data.data;
		nameEdit = list.name ?? '';
		searchResults = null;
	}

	$effect(() => {
		void listId;
		load();
	});

	async function saveName() {
		savingName = true;
		const { error: err } = await getClient().PUT('/api/lists/{list}', {
			params: { path: { list: listId } },
			body: { name: nameEdit.trim() }
		});
		savingName = false;
		if (err) {
			alert(err.error ?? 'Update failed');
			return;
		}
		await load();
	}

	async function doSearch(event: SubmitEvent) {
		event.preventDefault();
		const { data, error: err } = await getClient().GET('/api/lists/{list}/search', {
			params: { path: { list: listId }, query: { value: search } }
		});
		if (err) {
			alert(err.error ?? 'Search failed');
			return;
		}
		searchResults = data?.data ?? [];
	}

	async function createElement(event: SubmitEvent) {
		event.preventDefault();
		creating = true;
		createError = '';
		const body = {
			id: elId.trim(),
			name: elName.trim(),
			description: elDesc.trim(),
			index: Number.parseInt(elIndex, 10) || 0,
			aliases: elAliases
				.split(',')
				.map((a) => a.trim())
				.filter(Boolean),
			...(elActiveTo ? { activeTo: new Date(elActiveTo).toISOString() } : {})
		};
		const { error: err } = await getClient().POST('/api/lists/{list}', {
			params: { path: { list: listId } },
			body
		});
		creating = false;
		if (err) {
			createError = err.error ?? 'Create failed';
			return;
		}
		elId = '';
		elName = '';
		elDesc = '';
		elIndex = '0';
		elAliases = '';
		elActiveTo = '';
		await load();
	}

	function startEdit(el: ListElement) {
		editing = String(el.type);
		editName = el.name;
		editDesc = el.description;
		editIndex = String(el.index);
		editAliases = el.aliases.join(', ');
		editActiveTo = el.activeTo ? el.activeTo.slice(0, 16) : '';
	}

	async function saveElement() {
		if (!editing) return;
		const { error: err } = await getClient().PUT('/api/lists/{list}/{element}', {
			params: { path: { list: listId, element: editing } },
			body: {
				name: editName.trim(),
				description: editDesc.trim(),
				index: Number.parseInt(editIndex, 10) || 0,
				aliases: editAliases
					.split(',')
					.map((a) => a.trim())
					.filter(Boolean),
				...(editActiveTo ? { activeTo: new Date(editActiveTo).toISOString() } : {})
			}
		});
		if (err) {
			alert(err.error ?? 'Update failed');
			return;
		}
		editing = null;
		await load();
	}

	async function deleteElement(id: string) {
		if (!confirm(`Delete element “${id}”?`)) return;
		const { error: err } = await getClient().DELETE('/api/lists/{list}/{element}', {
			params: { path: { list: listId, element: id } }
		});
		if (err) {
			alert(err.error ?? 'Delete failed');
			return;
		}
		await load();
	}

	let displayElements = $derived(searchResults ?? list?.elements ?? []);
</script>

<div class="flex flex-col gap-6">
	<p class="text-sm">
		<a href="/lists" class="text-(--color-primary) underline">Lists</a>
	</p>

	{#if loading}
		<p class="text-sm text-gray-500">Loading…</p>
	{:else if error}
		<p class="text-sm text-red-600">{error}</p>
	{:else if list}
		<h1 class="text-2xl font-semibold">{list.name}</h1>
		<p class="font-mono text-xs text-gray-500">{list.type ?? listId}</p>

		<section class="flex flex-wrap items-end gap-2">
			<label class="flex flex-col gap-1 text-sm">
				<span>Name</span>
				<input
					bind:value={nameEdit}
					class="rounded border border-gray-300 px-3 py-2 dark:border-gray-600"
				/>
			</label>
			<button
				type="button"
				class="rounded border border-gray-300 px-3 py-2 text-sm"
				disabled={savingName}
				onclick={saveName}
			>
				Save name
			</button>
		</section>

		<section class="rounded-lg border border-gray-200 p-4 dark:border-gray-700">
			<h2 class="mb-3 text-lg font-medium">Search elements</h2>
			<form class="flex gap-2" onsubmit={doSearch}>
				<input
					bind:value={search}
					placeholder="Name or alias"
					class="flex-1 rounded border border-gray-300 px-3 py-2 dark:border-gray-600"
				/>
				<button type="submit" class="rounded bg-(--color-primary) px-4 py-2 text-sm text-white">
					Search
				</button>
				{#if searchResults}
					<button
						type="button"
						class="rounded border border-gray-300 px-3 py-2 text-sm"
						onclick={() => (searchResults = null)}
					>
						Clear
					</button>
				{/if}
			</form>
		</section>

		<section class="rounded-lg border border-gray-200 p-4 dark:border-gray-700">
			<h2 class="mb-3 text-lg font-medium">Create element</h2>
			<form class="grid gap-2 sm:grid-cols-2" onsubmit={createElement}>
				<label class="flex flex-col gap-1 text-sm">
					<span>ID</span>
					<input bind:value={elId} required class="rounded border border-gray-300 px-3 py-2" />
				</label>
				<label class="flex flex-col gap-1 text-sm">
					<span>Name</span>
					<input bind:value={elName} required class="rounded border border-gray-300 px-3 py-2" />
				</label>
				<label class="flex flex-col gap-1 text-sm sm:col-span-2">
					<span>Description</span>
					<input bind:value={elDesc} class="rounded border border-gray-300 px-3 py-2" />
				</label>
				<label class="flex flex-col gap-1 text-sm">
					<span>Index</span>
					<input bind:value={elIndex} type="number" class="rounded border border-gray-300 px-3 py-2" />
				</label>
				<label class="flex flex-col gap-1 text-sm">
					<span>Active to</span>
					<input
						bind:value={elActiveTo}
						type="datetime-local"
						class="rounded border border-gray-300 px-3 py-2"
					/>
				</label>
				<label class="flex flex-col gap-1 text-sm sm:col-span-2">
					<span>Aliases (comma-separated)</span>
					<input bind:value={elAliases} class="rounded border border-gray-300 px-3 py-2" />
				</label>
				<button
					type="submit"
					disabled={creating}
					class="rounded bg-(--color-primary) px-4 py-2 text-sm text-white sm:col-span-2 disabled:opacity-50"
				>
					{creating ? 'Creating…' : 'Create element'}
				</button>
			</form>
			{#if createError}
				<p class="mt-2 text-sm text-red-600">{createError}</p>
			{/if}
		</section>

		<section>
			<h2 class="mb-3 text-lg font-medium">
				Elements {#if searchResults}(search results){/if}
			</h2>
			{#if displayElements.length === 0}
				<p class="text-sm text-gray-500">No elements</p>
			{:else}
				<ul class="divide-y divide-gray-200 rounded border border-gray-200 text-sm">
					{#each displayElements as el (String(el.type))}
						<li class="px-4 py-3">
							{#if editing === String(el.type)}
								<div class="grid gap-2 sm:grid-cols-2">
									<input bind:value={editName} class="rounded border px-2 py-1" />
									<input bind:value={editIndex} type="number" class="rounded border px-2 py-1" />
									<input
										bind:value={editDesc}
										class="rounded border px-2 py-1 sm:col-span-2"
										placeholder="Description"
									/>
									<input
										bind:value={editAliases}
										class="rounded border px-2 py-1 sm:col-span-2"
										placeholder="Aliases"
									/>
									<input
										bind:value={editActiveTo}
										type="datetime-local"
										class="rounded border px-2 py-1"
									/>
									<div class="flex gap-2">
										<button type="button" class="text-(--color-primary) underline" onclick={saveElement}>
											Save
										</button>
										<button
											type="button"
											class="text-gray-500 underline"
											onclick={() => (editing = null)}
										>
											Cancel
										</button>
									</div>
								</div>
							{:else}
								<div class="flex flex-wrap items-start justify-between gap-2">
									<div>
										<div class="font-medium">{el.name}</div>
										<div class="font-mono text-xs text-gray-500">{el.type}</div>
										<p class="text-gray-600">{el.description}</p>
										{#if el.aliases.length}
											<p class="text-xs text-gray-500">Aliases: {el.aliases.join(', ')}</p>
										{/if}
									</div>
									<div class="flex gap-2">
										<button
											type="button"
											class="text-(--color-primary) underline"
											onclick={() => startEdit(el)}
										>
											Edit
										</button>
										<button
											type="button"
											class="text-red-600 underline"
											onclick={() => deleteElement(String(el.type))}
										>
											Delete
										</button>
									</div>
								</div>
							{/if}
						</li>
					{/each}
				</ul>
			{/if}
		</section>
	{/if}
</div>
