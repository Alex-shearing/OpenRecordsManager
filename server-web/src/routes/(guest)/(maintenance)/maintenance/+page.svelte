<script lang="ts">
	import { DatabaseController } from '#lib/api/index.js';
	import { getApiClient } from '#lib/api-client.js';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { t } from '#lib/i18n/catalog.js';

	let view = $state<'checking' | 'upgrade' | 'unavailable' | 'ready'>('checking');

	const pollIntervalMs = 3000;

	async function checkStatus() {
		try {
			const { data, error } = await DatabaseController.status({ client: getApiClient() });
			if (error) {
				return 'unavailable';
			}
			if (data.data.state === 'READY') {
				await goto(page.url.searchParams.get('redirect') || '');
				return 'ready';
			}
			return 'upgrade';
		} catch {
			return 'unavailable';
		}
	}

	$effect(() => {
		let cancelled = false;
		let timer = setTimeout(poll, 100);

		async function poll() {
			view = await checkStatus();
			if (cancelled || view === 'ready') return;
			timer = setTimeout(poll, pollIntervalMs);
		}

		return () => {
			cancelled = true;
			if (timer) clearTimeout(timer);
		};
	});
</script>

<div class="card text-center">
	<div class="card-body">
		{#if view === 'ready'}
			<h1 class="mb-3 text-2xl font-semibold">{t('web.maintenance.ready')}</h1>

			<a href="/login" class="text-link text-sm">{t('web.maintenance.continue_sign_in')}</a>
		{:else}
			<h1 class="mb-3 text-2xl font-semibold">{t(`web.maintenance.${view}`)}</h1>
			<p class="mb-2 text-muted-foreground">{t(`web.maintenance.${view}_hint`)}</p>
		{/if}
	</div>
</div>
