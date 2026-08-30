<script lang="ts">
	import { getClient } from '$lib';
	import { parseOptionalJson } from '$lib/api';
	import type { components } from '$lib/types/schema';

	type AuthProvider = components['schemas']['AuthProviderListResponse'];

	let providers = $state<AuthProvider[]>([]);
	let error = $state('');
	let loading = $state(true);

	let name = $state('');
	let typeId = $state('auth_local:local_auth');
	let type = $state<'INPUT' | 'REDIRECT'>('INPUT');
	let settingsJson = $state('{}');
	let creating = $state(false);
	let createError = $state('');

	async function load() {
		loading = true;
		const { data, error: err } = await getClient().GET('/api/auth/providers');
		loading = false;
		if (err || !data?.data) {
			error = err?.error ?? 'Failed to load providers';
			providers = [];
			return;
		}
		error = '';
		providers = data.data;
	}

	$effect(() => {
		load();
	});

	async function create(event: SubmitEvent) {
		event.preventDefault();
		creating = true;
		createError = '';
		let settings: Record<string, unknown> = {};
		try {
			const parsed = parseOptionalJson(settingsJson);
			if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
				settings = parsed as Record<string, unknown>;
			} else if (settingsJson.trim() && settingsJson.trim() !== '{}') {
				createError = 'Settings must be a JSON object';
				creating = false;
				return;
			}
		} catch {
			createError = 'Invalid settings JSON';
			creating = false;
			return;
		}

		const { error: err } = await getClient().PUT('/api/auth/providers', {
			body: {
				name: name.trim() || undefined,
				typeId: typeId.trim() || undefined,
				type,
				settings
			}
		});
		creating = false;
		if (err) {
			createError = err.error ?? 'Create failed';
			return;
		}
		name = '';
		settingsJson = '{}';
		await load();
	}
</script>

<div class="flex flex-col gap-6">
	<h1 class="text-2xl font-semibold">Auth providers</h1>

	{#if loading}
		<p class="text-sm text-gray-500">Loading…</p>
	{:else if error}
		<p class="text-sm text-red-600">{error}</p>
	{:else}
		<ul class="divide-y divide-gray-200 rounded border border-gray-200 text-sm dark:border-gray-700">
			{#each providers as provider (provider.id)}
				<li class="px-4 py-3">
					<div class="font-medium">{provider.name || provider.id}</div>
					<div class="font-mono text-xs text-gray-500">
						{provider.id} · {provider.type?.type ?? provider.type?.id}
					</div>
				</li>
			{:else}
				<li class="px-4 py-3 text-gray-500">No providers configured</li>
			{/each}
		</ul>
	{/if}

	<section class="rounded-lg border border-gray-200 p-4 dark:border-gray-700">
		<h2 class="mb-3 text-lg font-medium">Create provider</h2>
		<form class="flex flex-col gap-3" onsubmit={create}>
			<label class="flex flex-col gap-1 text-sm">
				<span>Name</span>
				<input bind:value={name} class="rounded border border-gray-300 px-3 py-2" />
			</label>
			<label class="flex flex-col gap-1 text-sm">
				<span>Type ID</span>
				<input
					bind:value={typeId}
					required
					placeholder="auth_local:local_auth"
					class="rounded border border-gray-300 px-3 py-2 font-mono"
				/>
			</label>
			<label class="flex flex-col gap-1 text-sm">
				<span>Kind</span>
				<select bind:value={type} class="rounded border border-gray-300 px-3 py-2">
					<option value="INPUT">INPUT</option>
					<option value="REDIRECT">REDIRECT</option>
				</select>
			</label>
			<label class="flex flex-col gap-1 text-sm">
				<span>Settings (JSON)</span>
				<textarea
					bind:value={settingsJson}
					rows="4"
					class="rounded border border-gray-300 px-3 py-2 font-mono text-xs"
				></textarea>
			</label>
			{#if createError}
				<p class="text-sm text-red-600">{createError}</p>
			{/if}
			<button
				type="submit"
				disabled={creating}
				class="self-start rounded bg-(--color-primary) px-4 py-2 text-sm text-white disabled:opacity-50"
			>
				{creating ? 'Creating…' : 'Create provider'}
			</button>
		</form>
	</section>
</div>
