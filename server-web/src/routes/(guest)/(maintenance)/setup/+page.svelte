<script lang="ts">
	import { DatabaseController, SetupController, type ApiErrorResponse } from '#lib/api/index.js';
	import { getApiClient } from '#lib/api-client.js';
	import { t, tApiErrorResponse } from '#lib/i18n/catalog.js';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';

	let { data } = $props();

	// svelte-ignore state_referenced_locally
	let statusData = $state(data.status);
	let upgrading = $state(false);
	let upgradeError = $state<ApiErrorResponse>();
	let validating = $state(false);
	let validateError = $state<ApiErrorResponse>();
	let validationResult = $state<boolean>();
	let completing = $state(false);
	let completeError = $state<ApiErrorResponse>();
	let username = $state('admin');
	let password = $state('');
	let formError = $state<string>();

	const busy = $derived(upgrading || validating || completing);
	const status = $derived(statusData.data?.data);
	const needsUpgrade = $derived(status?.state === 'UPGRADE_REQUIRED');
	const needsAdmin = $derived(status?.state === 'SETUP_REQUIRED');
	const ready = $derived(status?.state === 'READY');

	async function refreshStatus() {
		statusData = await DatabaseController.getSchemaMigrationStatus({ client: getApiClient() });
	}

	async function upgrade() {
		upgrading = true;
		upgradeError = undefined;
		validationResult = undefined;
		validateError = undefined;

		const result = await DatabaseController.upgradeSchema({ client: getApiClient() });
		upgrading = false;

		if (result.error) {
			upgradeError = result.error;
			return;
		}

		statusData = result;
	}

	async function validateSchema() {
		validating = true;
		validateError = undefined;
		validationResult = undefined;

		const result = await DatabaseController.validateSchema({ client: getApiClient() });
		validating = false;

		if (result.error) {
			validateError = result.error;
			return;
		}

		validationResult = result.data.data;
	}

	async function completeSetup(event: Event) {
		event.preventDefault();
		formError = undefined;
		completeError = undefined;

		if (password.length < 8) {
			formError = t('web.setup.password_too_short');
			return;
		}

		completing = true;
		const result = await SetupController.completeSetup({
			client: getApiClient(),
			body: { username: username.trim(), password },
		});
		completing = false;

		if (result.error) {
			completeError = result.error;
			return;
		}

		await refreshStatus();
		const redirect = page.url.searchParams.get('redirect');
		await goto(redirect ? decodeURIComponent(redirect) : '/login');
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
				{t('web.setup.current_version', status?.currentVersion ?? 'unknown')}
			</span>
		</p>

		{#if statusData.error}
			<p class="text-destructive">{tApiErrorResponse(statusData.error)}</p>
		{:else if status}
			{#if needsUpgrade}
				{#if status.pendingMigrations.length}
					<p class="mb-2 text-sm font-medium">{t('web.setup.pending')}</p>
					<ul class="mb-6 list-inside list-disc text-sm text-foreground">
						{#each status.pendingMigrations as migration (migration)}
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
			{:else}
				{#if status.migrationsApplied >= 0}
					<p class="mb-4 text-sm text-foreground">
						{t('web.setup.migrations_applied', status.migrationsApplied)}
					</p>
				{:else}
					<p class="mb-4 text-sm text-foreground">{t('web.setup.up_to_date')}</p>
				{/if}

				{#if needsAdmin}
					{#if !status.writable}
						<p class="mb-4 text-sm text-destructive">{t('web.setup.not_writable')}</p>
					{:else}
						<form class="mx-auto max-w-sm space-y-4 text-left" onsubmit={completeSetup}>
							<p class="text-sm font-medium">{t('web.setup.admin_title')}</p>
							<p class="text-sm text-hint">{t('web.setup.admin_intro')}</p>

							<label class="block text-sm">
								<span class="mb-1 block">{t('input_auth_provider.auth_local.local_auth.schema.username.title')}</span>
								<input
									class="input w-full"
									type="text"
									name="username"
									autocomplete="username"
									required
									minlength="1"
									maxlength="128"
									bind:value={username}
									disabled={busy}
								/>
							</label>

							<label class="block text-sm">
								<span class="mb-1 block">{t('input_auth_provider.auth_local.local_auth.schema.password.title')}</span>
								<input
									class="input w-full"
									type="password"
									name="password"
									autocomplete="new-password"
									required
									minlength="8"
									maxlength="256"
									bind:value={password}
									disabled={busy}
								/>
							</label>

							{#if formError}
								<p class="text-sm text-destructive">{formError}</p>
							{/if}
							{#if completeError}
								<p class="text-sm text-destructive">{tApiErrorResponse(completeError)}</p>
							{/if}

							<button type="submit" class="btn-primary w-full" disabled={busy}>
								{completing ? t('web.setup.completing') : t('web.setup.complete')}
							</button>
						</form>
					{/if}
				{:else}
					<p class="mb-4 text-sm text-foreground">{t('web.setup.complete_ok')}</p>
					<a href="/login" class="text-link text-sm">{t('web.maintenance.continue_sign_in')}</a>
				{/if}
			{/if}

			{#if ready}
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
		{/if}
	</div>
</div>
