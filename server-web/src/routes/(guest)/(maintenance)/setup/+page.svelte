<script lang="ts">
	import { DatabaseController } from '$lib/api';
	import { getApiClient } from '$lib/api-client';
	import { t } from '$lib/i18n/catalog';

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

<!-- TODO: redesign this page to always have the current schema number visible and include a button that can be pressed to run the database schema validator -->
<div class="card">
	<div class="card-header">
		<h1 class="text-2xl font-semibold">{t('web.setup.title')}</h1>
		<p class="mt-1 text-hint">{t('web.setup.intro')}</p>
	</div>

	<div class="card-body">
		{#if !status.success}
			<p class="text-destructive">{t('web.setup.load_failed')}</p>
		{:else if status.data.state === 'READY'}
			<p class="mb-4 text-sm text-foreground">{t('web.setup.up_to_date', status.data.currentVersion || 'unknown')}</p>
			<a href="/login" class="text-link text-sm">{t('web.maintenance.continue_sign_in')}</a>
		{:else}
			<p class="mb-4 text-sm text-foreground">
				{status.data.message || ''}
			</p>
			{#if status.data.currentVersion}
				<p class="mb-2 text-sm">
					<span class="font-medium">{t('web.setup.current_version', status.data.currentVersion)}</span>
				</p>
			{/if}
			{#if status.data.pendingMigrations.length}
				<p class="mb-2 text-sm font-medium">{t('web.setup.pending')}</p>
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
				{upgrading ? t('web.setup.upgrading') : t('web.setup.upgrade')}
			</button>
		{/if}
	</div>
</div>
