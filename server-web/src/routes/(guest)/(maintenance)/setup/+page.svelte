<script lang="ts">
	import { DatabaseController, type ApiErrorResponse } from '#lib/api/index.js';
	import { getApiClient } from '#lib/api-client.js';
	import { t, tApiErrorResponse } from '#lib/i18n/catalog.js';

	let { data } = $props();

	// svelte-ignore state_referenced_locally
	let statusData = $state(data.status);
	let upgrading = $state(false);
	let upgradeError = $state<ApiErrorResponse>();
	let validating = $state(false);
	let validateError = $state<ApiErrorResponse>();
	let validationResult = $state<boolean>();

	const busy = $derived(upgrading || validating);

	async function upgrade() {
		upgrading = true;
		upgradeError = undefined;
		validationResult = undefined;
		validateError = undefined;

		const result = await DatabaseController.upgrade({ client: getApiClient() });
		upgrading = false;

		if (result.error) {
			upgradeError = result.error;
			return;
		}

		const status = await DatabaseController.status({ client: getApiClient() });
		if (status.error) {
			upgradeError = status.error;
			return;
		}

		statusData = status;
	}

	async function validateSchema() {
		validating = true;
		validateError = undefined;
		validationResult = undefined;

		const result = await DatabaseController.validate({ client: getApiClient() });
		validating = false;

		if (result.error) {
			validateError = result.error;
			return;
		}

		validationResult = result.data.data;
	}
</script>

<div class="card text-center">
	<div class="card-header">
		<h1 class="text-2xl font-semibold">{t('web.setup.title')}</h1>
		<p class="mt-1 text-hint">{t('web.setup.intro')}</p>
	</div>

	<div class="card-body">
		<p class="mb-6 text-sm">
			<span class="font-medium">
				{t('web.setup.current_version', statusData.data?.data?.currentVersion ?? 'unknown')}
			</span>
		</p>

		{#if statusData.error}
			<p class="text-destructive">{tApiErrorResponse(statusData.error)}</p>
		{:else}
			{@const migrationData = statusData.data.data}
			{#if migrationData.state === 'READY'}
				{#if migrationData.migrationsApplied >= 0}
					<p class="mb-4 text-sm text-foreground">
						{t('web.setup.migrations_applied', migrationData.migrationsApplied)}
					</p>
				{:else}
					<p class="mb-4 text-sm text-foreground">{t('web.setup.up_to_date')}</p>
				{/if}
				<a href="/login" class="text-link text-sm">{t('web.maintenance.continue_sign_in')}</a>
			{:else}
				{#if migrationData.pendingMigrations.length}
					<p class="mb-2 text-sm font-medium">{t('web.setup.pending')}</p>
					<ul class="mb-6 list-inside list-disc text-sm text-foreground">
						{#each migrationData.pendingMigrations as migration (migration)}
							<li>{migration}</li>
						{/each}
					</ul>
				{/if}
				{#if upgradeError}
					<p class="mb-4 text-sm text-destructive">{tApiErrorResponse(upgradeError)}</p>
				{/if}
				<button type="button" class="btn-primary" disabled={busy} onclick={upgrade}>
					{upgrading ? t('web.setup.upgrading') : t('web.setup.upgrade')}
				</button>
			{/if}

			<div class="mt-8 border-t border-border pt-6">
				{#if validateError}
					<p class="mb-4 text-sm text-destructive">{tApiErrorResponse(validateError)}</p>
				{/if}
				{#if validationResult}
					<p class="mb-4 text-sm text-foreground">{t('web.setup.validate_ok')}</p>
				{/if}
				<button type="button" class="btn-secondary" disabled={busy} onclick={validateSchema}>
					{validating ? t('web.setup.validating') : t('web.setup.validate')}
				</button>
			</div>
		{/if}
	</div>
</div>
