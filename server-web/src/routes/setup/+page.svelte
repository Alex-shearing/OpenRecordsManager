<script lang="ts">
	import { getClient } from '$lib';
	import { goto } from '$app/navigation';
	import { config } from '$lib/config.svelte';

	const branding = config.getConfig();

	let statusPromise = $state(getClient().GET('/api/database/status'));
	let upgrading = $state(false);
	let upgradeError = $state<string | null>(null);
	let validating = $state(false);
	let validationMessage = $state<string | null>(null);

	async function upgrade() {
		upgrading = true;
		upgradeError = null;
		const { data, error } = await getClient().POST('/api/database/upgrade');
		upgrading = false;

		if (error) {
			upgradeError = error.error;
			return;
		}

		if (data.data.state === 'READY') {
			await goto('/login');
			return;
		}

		statusPromise = Promise.resolve({ data, error: undefined, response: new Response() });
	}

	async function validate() {
		validating = true;
		validationMessage = null;
		const { data, error } = await getClient().POST('/api/database/validate');
		validating = false;
		if (error) {
			validationMessage = error.error ?? 'Validation failed';
			return;
		}
		const result = data?.data;
		validationMessage = result
			? `${result.validated ? 'Valid' : 'Invalid'}${result.message ? `: ${result.message}` : ''}`
			: 'No validation result';
	}
</script>

<div class="mx-auto flex min-h-screen max-w-lg flex-col justify-center px-4 py-12">
	<h1 class="mb-2 text-2xl font-semibold">{branding.productName}</h1>
	<p class="mb-8 text-sm text-gray-600">Database schema upgrade</p>

	{#await statusPromise}
		<p class="text-sm text-gray-500">Checking schema status…</p>
	{:then { data, error }}
		{#if error}
			<p class="text-red-600">Unable to load schema status.</p>
		{:else if data.data.state === 'READY'}
			<p class="mb-4 text-sm text-gray-700">Schema is up to date.</p>
			<div class="mb-4 flex flex-wrap gap-2">
				<button
					type="button"
					class="rounded border border-gray-300 px-4 py-2 text-sm disabled:opacity-50"
					disabled={validating}
					onclick={validate}
				>
					{validating ? 'Validating…' : 'Validate schema'}
				</button>
				<a href="/login" class="self-center text-sm font-medium text-(--color-primary) underline"
					>Continue to sign in</a
				>
			</div>
			{#if validationMessage}
				<p class="text-sm text-gray-700">{validationMessage}</p>
			{/if}
		{:else}
			<p class="mb-4 text-sm text-gray-700">
				{data.data.message ||
					'A database schema upgrade is required before the application can be used.'}
			</p>
			{#if data.data.currentVersion}
				<p class="mb-2 text-sm">
					<span class="font-medium">Current version:</span>
					{data.data.currentVersion}
				</p>
			{/if}
			{#if data.data.pendingMigrations.length}
				<p class="mb-2 text-sm font-medium">Pending migrations:</p>
				<ul class="mb-6 list-disc pl-5 text-sm text-gray-700">
					{#each data.data.pendingMigrations as migration (migration)}
						<li>{migration}</li>
					{/each}
				</ul>
			{/if}
			{#if upgradeError}
				<p class="mb-4 text-sm text-red-600">{upgradeError}</p>
			{/if}
			<div class="flex flex-wrap gap-2">
				<button
					type="button"
					class="rounded bg-(--color-primary) px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
					disabled={upgrading}
					onclick={upgrade}
				>
					{upgrading ? 'Upgrading…' : 'Upgrade schema'}
				</button>
				<button
					type="button"
					class="rounded border border-gray-300 px-4 py-2 text-sm disabled:opacity-50"
					disabled={validating}
					onclick={validate}
				>
					{validating ? 'Validating…' : 'Validate schema'}
				</button>
			</div>
			{#if validationMessage}
				<p class="mt-3 text-sm text-gray-700">{validationMessage}</p>
			{/if}
		{/if}
	{:catch}
		<p class="text-red-600">Unable to load schema status.</p>
	{/await}
</div>
