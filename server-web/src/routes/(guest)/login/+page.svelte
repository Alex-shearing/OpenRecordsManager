<script lang="ts">
	import LoginForm from '$lib/components/LoginForm.svelte';
	import { page } from '$app/state';
	import { t, tApiErrorResponse } from '$lib/i18n/catalog';

	let { data } = $props();

	const error = page.url.searchParams.get('error');
	const authError = error ? tApiErrorResponse({ error }) : null;
</script>

<div class="card">
	<div class="card-header">
		<h1 class="text-2xl font-semibold">{t('web.login.title')}</h1>
	</div>

	<div class="card-body">
		{#if authError}
			<p class="mb-4 text-sm text-destructive" role="alert">{authError}</p>
		{/if}
		{#if data.providersError}
			<p class="text-sm text-destructive">
				{tApiErrorResponse(data.providersError)}
			</p>
		{:else if data.inputProviders.length === 0 && data.redirectProviders.length === 0}
			<p class="text-hint">{t('web.login.no_options')}</p>
		{:else}
			<LoginForm inputProviders={data.inputProviders} redirectProviders={data.redirectProviders} />
		{/if}
	</div>
</div>
