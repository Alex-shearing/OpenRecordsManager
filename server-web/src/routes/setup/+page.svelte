<script lang="ts">
	import { DatabaseController } from '$lib';
	import { goto } from '$app/navigation';

	let { data } = $props();

	let statusPromise = $state(DatabaseController.status());
	let upgrading = $state(false);
	let upgradeError = $state<string | null>(null);

	async function upgrade() {
		upgrading = true;
		upgradeError = null;
		const { data, error } = await DatabaseController.upgrade();
		upgrading = false;

		if (error) {
			upgradeError = error.error;
			return;
		}

		if (data.data.state === 'READY') {
			await goto('/login');
			return;
		}

		statusPromise = DatabaseController.status();
	}
</script>

<div class="mx-auto flex min-h-screen max-w-lg flex-col justify-center px-4 py-12">
	<h1 class="mb-2 text-2xl font-semibold">{data.branding.productName}</h1>
	<p class="mb-8 text-hint">Database schema upgrade</p>

	{#await statusPromise}
		<p class="text-hint">Checking schema status…</p>
	{:then { data, error }}
		{#if error}
			<p class="text-destructive">Unable to load schema status.</p>
		{:else if data.data.state === 'READY'}
			<p class="mb-4 text-sm text-foreground">Schema is up to date.</p>
			<a href="/login" class="text-link text-sm">Continue to sign in</a>
		{:else}
			<p class="mb-4 text-sm text-foreground">
				{data.data.message || 'A database schema upgrade is required before the application can be used.'}
			</p>
			{#if data.data.currentVersion}
				<p class="mb-2 text-sm"><span class="font-medium">Current version:</span> {data.data.currentVersion}</p>
			{/if}
			{#if data.data.pendingMigrations.length}
				<p class="mb-2 text-sm font-medium">Pending migrations:</p>
				<ul class="mb-6 list-disc pl-5 text-sm text-foreground">
					{#each data.data.pendingMigrations as migration (migration)}
						<li>{migration}</li>
					{/each}
				</ul>
			{/if}
			{#if upgradeError}
				<p class="mb-4 text-sm text-destructive">{upgradeError}</p>
			{/if}
			<button type="button" class="btn-primary" disabled={upgrading} onclick={upgrade}>
				{upgrading ? 'Upgrading…' : 'Upgrade schema'}
			</button>
		{/if}
	{:catch}
		<p class="text-destructive">Unable to load schema status.</p>
	{/await}
</div>
