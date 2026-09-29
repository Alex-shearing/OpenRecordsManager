<script lang="ts">
	import { DatabaseController, type ApiErrorResponse } from '$lib/api';
	import { getApiClient } from '$lib/api-client';
	import { t, tApiErrorResponse } from '$lib/i18n/catalog';

	let { data } = $props();

	// svelte-ignore state_referenced_locally
	let statusData = $state(data.status);
	let upgrading = $state(false);
	let upgradeError = $state<ApiErrorResponse>();

	const status = $derived(statusData.data || statusData.error);

	async function upgrade() {
		upgrading = true;
		upgradeError = undefined;
		const client = getApiClient();
		const result = await DatabaseController.upgrade({ client });
		upgrading = false;

		if (result.error) {
			upgradeError = result.error;
			return;
		}

		// Prefer the upgrade payload (includes migrationsApplied); fall back to a status re-fetch.
		statusData = result.data != null ? result : await DatabaseController.status({ client });
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
			{#if status.data.migrationsApplied >= 0}
				<p class="mb-4 text-sm text-foreground">
					{t('web.setup.migrations_applied', status.data.migrationsApplied)}
				</p>
			{:else}
				<p class="mb-4 text-sm text-foreground">{t('web.setup.up_to_date', status.data.currentVersion || 'unknown')}</p>
			{/if}
			<a href="/login" class="text-link text-sm">{t('web.maintenance.continue_sign_in')}</a>
		{:else}
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
				<p class="mb-4 text-sm text-destructive">{tApiErrorResponse(upgradeError)}</p>
			{/if}
			<button type="button" class="btn-primary" disabled={upgrading} onclick={upgrade}>
				{upgrading ? t('web.setup.upgrading') : t('web.setup.upgrade')}
			</button>
		{/if}
	</div>
</div>
