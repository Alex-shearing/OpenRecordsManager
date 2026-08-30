<script lang="ts">
	import { getClient } from '$lib';
	import { formatValue, parseOptionalJson } from '$lib/api';
	import type { components } from '$lib/types/schema';

	type SimpleProperty = components['schemas']['SimpleObjectPropertyResponse'];
	type ObjectProperty = components['schemas']['ObjectPropertyResponse'];
	type SimpleList = components['schemas']['SimpleListTypeResponse'];

	const PROPERTY_TYPES = [
		'string',
		'number',
		'decimal',
		'boolean',
		'date',
		'list_item',
		'list_multiple',
		'calculated'
	];

	let properties = $state<SimpleProperty[]>([]);
	let lists = $state<SimpleList[]>([]);
	let error = $state('');
	let loading = $state(true);

	let newId = $state('');
	let newName = $state('');
	let newDesc = $state('');
	let newType = $state('string');
	let newListType = $state('');
	let newValidator = $state('');
	let newSecurity = $state('');
	let newDefault = $state('');
	let newUserHidden = $state(false);
	let creating = $state(false);
	let createError = $state('');

	let editing = $state<ObjectProperty | null>(null);
	let editName = $state('');
	let editDesc = $state('');
	let editValidator = $state('');
	let editSecurity = $state('');
	let editDefault = $state('');
	let editUserHidden = $state(false);

	async function load() {
		loading = true;
		const [propsRes, listsRes] = await Promise.all([
			getClient().GET('/api/object_properties'),
			getClient().GET('/api/lists')
		]);
		loading = false;
		if (propsRes.error || !propsRes.data?.data) {
			error = propsRes.error?.error ?? 'Failed to load properties';
			properties = [];
			return;
		}
		error = '';
		properties = propsRes.data.data;
		lists = listsRes.data?.data ?? [];
	}

	$effect(() => {
		load();
	});

	async function create(event: SubmitEvent) {
		event.preventDefault();
		creating = true;
		createError = '';
		const body = {
			id: newId.trim(),
			name: newName.trim(),
			description: newDesc.trim(),
			type: { name: newType },
			userHidden: newUserHidden,
			...(newListType ? { listType: newListType } : {}),
			...(newValidator.trim() ? { validator: newValidator.trim() } : {}),
			...(newSecurity.trim() ? { securityFilter: newSecurity.trim() } : {}),
			...(newDefault.trim() ? { defaultValue: parseOptionalJson(newDefault) } : {})
		};
		const { error: err } = await getClient().POST('/api/object_properties', { body });
		creating = false;
		if (err) {
			createError = err.error ?? 'Create failed';
			return;
		}
		newId = '';
		newName = '';
		newDesc = '';
		newType = 'string';
		newListType = '';
		newValidator = '';
		newSecurity = '';
		newDefault = '';
		newUserHidden = false;
		await load();
	}

	async function startEdit(id: string) {
		const { data, error: err } = await getClient().GET('/api/object_properties/{id}', {
			params: { path: { id } }
		});
		if (err || !data?.data) {
			alert(err?.error ?? 'Failed to load property');
			return;
		}
		const prop = data.data;
		editing = prop;
		editName = prop.name;
		editDesc = prop.description;
		editValidator = prop.validator ?? '';
		editSecurity = prop.securityFilter ?? '';
		editDefault =
			prop.defaultValue === undefined || prop.defaultValue === null
				? ''
				: typeof prop.defaultValue === 'string'
					? prop.defaultValue
					: JSON.stringify(prop.defaultValue);
		editUserHidden = Boolean(prop.userHidden);
	}

	async function saveEdit() {
		if (!editing) return;
		const { error: err } = await getClient().PUT('/api/object_properties/{id}', {
			params: { path: { id: editing.id } },
			body: {
				name: editName.trim(),
				description: editDesc.trim(),
				userHidden: editUserHidden,
				...(editValidator.trim() ? { validator: editValidator.trim() } : {}),
				...(editSecurity.trim() ? { securityFilter: editSecurity.trim() } : {}),
				...(editDefault.trim() ? { defaultValue: parseOptionalJson(editDefault) } : {})
			}
		});
		if (err) {
			alert(err.error ?? 'Update failed');
			return;
		}
		editing = null;
		await load();
	}

	async function remove(id: string) {
		if (!confirm(`Delete property “${id}”?`)) return;
		const { error: err } = await getClient().DELETE('/api/object_properties/{id}', {
			params: { path: { id } }
		});
		if (err) {
			alert(err.error ?? 'Delete failed');
			return;
		}
		await load();
	}
</script>

<div class="flex flex-col gap-6">
	<h1 class="text-2xl font-semibold">Object properties</h1>

	<section class="rounded-lg border border-gray-200 p-4 dark:border-gray-700">
		<h2 class="mb-3 text-lg font-medium">Create property</h2>
		<form class="grid gap-2 sm:grid-cols-2" onsubmit={create}>
			<label class="flex flex-col gap-1 text-sm">
				<span>ID</span>
				<input bind:value={newId} required class="rounded border border-gray-300 px-3 py-2" />
			</label>
			<label class="flex flex-col gap-1 text-sm">
				<span>Name</span>
				<input bind:value={newName} required class="rounded border border-gray-300 px-3 py-2" />
			</label>
			<label class="flex flex-col gap-1 text-sm sm:col-span-2">
				<span>Description</span>
				<input bind:value={newDesc} class="rounded border border-gray-300 px-3 py-2" />
			</label>
			<label class="flex flex-col gap-1 text-sm">
				<span>Type</span>
				<select bind:value={newType} class="rounded border border-gray-300 px-3 py-2">
					{#each PROPERTY_TYPES as t (t)}
						<option value={t}>{t}</option>
					{/each}
				</select>
			</label>
			<label class="flex flex-col gap-1 text-sm">
				<span>List type</span>
				<select bind:value={newListType} class="rounded border border-gray-300 px-3 py-2">
					<option value="">—</option>
					{#each lists as list (list.id)}
						<option value={list.id}>{list.name} ({list.id})</option>
					{/each}
				</select>
			</label>
			<label class="flex flex-col gap-1 text-sm">
				<span>Validator</span>
				<input bind:value={newValidator} class="rounded border border-gray-300 px-3 py-2" />
			</label>
			<label class="flex flex-col gap-1 text-sm">
				<span>Security filter</span>
				<input bind:value={newSecurity} class="rounded border border-gray-300 px-3 py-2" />
			</label>
			<label class="flex flex-col gap-1 text-sm sm:col-span-2">
				<span>Default value (JSON or text)</span>
				<input bind:value={newDefault} class="rounded border border-gray-300 px-3 py-2" />
			</label>
			<label class="flex items-center gap-2 text-sm sm:col-span-2">
				<input type="checkbox" bind:checked={newUserHidden} class="rounded" />
				User hidden
			</label>
			<button
				type="submit"
				disabled={creating}
				class="rounded bg-(--color-primary) px-4 py-2 text-sm text-white sm:col-span-2 disabled:opacity-50"
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
	{:else if properties.length === 0}
		<p class="text-sm text-gray-500">No object properties</p>
	{:else}
		<ul class="divide-y divide-gray-200 rounded border border-gray-200 text-sm">
			{#each properties as prop (prop.id)}
				<li class="px-4 py-3">
					{#if editing?.id === prop.id}
						<div class="grid gap-2 sm:grid-cols-2">
							<input bind:value={editName} class="rounded border px-2 py-1" />
							<label class="flex items-center gap-2">
								<input type="checkbox" bind:checked={editUserHidden} class="rounded" />
								User hidden
							</label>
							<input
								bind:value={editDesc}
								class="rounded border px-2 py-1 sm:col-span-2"
								placeholder="Description"
							/>
							<input bind:value={editValidator} class="rounded border px-2 py-1" placeholder="Validator" />
							<input
								bind:value={editSecurity}
								class="rounded border px-2 py-1"
								placeholder="Security filter"
							/>
							<input
								bind:value={editDefault}
								class="rounded border px-2 py-1 sm:col-span-2"
								placeholder="Default"
							/>
							{#if editing.defaultValue !== undefined}
								<p class="text-xs text-gray-500 sm:col-span-2">
									Current default: {formatValue(editing.defaultValue)}
								</p>
							{/if}
							<div class="flex gap-2 sm:col-span-2">
								<button type="button" class="text-(--color-primary) underline" onclick={saveEdit}>
									Save
								</button>
								<button type="button" class="text-gray-500 underline" onclick={() => (editing = null)}>
									Cancel
								</button>
							</div>
						</div>
					{:else}
						<div class="flex flex-wrap items-start justify-between gap-2">
							<div>
								<div class="font-medium">{prop.name}</div>
								<div class="font-mono text-xs text-gray-500">{prop.id} · {prop.type}</div>
							</div>
							<div class="flex gap-2">
								<button
									type="button"
									class="text-(--color-primary) underline"
									onclick={() => startEdit(prop.id)}
								>
									Edit
								</button>
								<button
									type="button"
									class="text-red-600 underline"
									onclick={() => remove(prop.id)}
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
</div>
