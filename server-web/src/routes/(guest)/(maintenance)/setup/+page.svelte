<script lang="ts">
	import { DatabaseController } from '$lib/api';
	import { getApiClient } from '$lib/api-client';

	let { data } = $props();

	// svelte-ignore state_referenced_locally
	let statusData = $state(data.status);
	let upgrading = $state(false);
	let upgradeError = $state<string | null>(null);

	const status = $derived(statusData.data || statusData.error);

	async function upgrade() {
		upgrading = true;
		upgradeError = null;
		const client = getApiClient();
		const { data, error } = await DatabaseController.upgrade({ client });
		upgrading = false;

		if (error) {
			upgradeError = error.error;
			return;
		}

		statusData = await DatabaseController.status({ client });
	}
</script>

<div class="card">
	<div class="card-header">
		<h1 class="text-2xl font-semibold">Database schema upgrade</h1>
		<p class="mt-1 text-hint">A schema upgrade is required before the application can be used.</p>
	</div>

	<div class="card-body">
		{#if !status.success}
			<p class="text-destructive">Unable to load schema status.</p>
		{:else if status.data.state === 'READY'}
			<p class="mb-4 text-sm text-foreground">Schema is up to date.</p>
			<a href="/login" class="text-link text-sm">Continue to sign in</a>
		{:else}
			<p class="mb-4 text-sm text-foreground">
				{status.data.message || 'A database schema upgrade is required before the application can be used.'}
			</p>
			{#if status.data.currentVersion}
				<p class="mb-2 text-sm">
					<span class="font-medium">Current version:</span>
					{status.data.currentVersion}
				</p>
			{/if}
			{#if status.data.pendingMigrations.length}
				<p class="mb-2 text-sm font-medium">Pending migrations:</p>
				<ul class="mb-6 list-disc pl-5 text-sm text-foreground">
					{#each status.data.pendingMigrations as migration (migration)}
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
	</div>
</div>
