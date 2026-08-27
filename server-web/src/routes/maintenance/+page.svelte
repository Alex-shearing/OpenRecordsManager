<script lang="ts">
	import { getClient } from '$lib';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { config } from '$lib/config.svelte';

	const branding = config.getConfig();
	const pollIntervalMs = 3000;

	type View = 'checking' | 'waiting' | 'unavailable' | 'ready';

	let view = $state<View>('checking');

	function returnPath() {
		const redirect = page.url.searchParams.get('redirect');
		if (redirect && redirect.startsWith('/') && !redirect.startsWith('//')) {
			return redirect;
		}
		return '/';
	}

	async function checkStatus() {
		try {
			const { data, error } = await getClient().GET('/api/database/status');
			if (error) {
				return 'unavailable';
			}
			if (data.data.state === 'READY') {
				await goto(returnPath());
				return 'ready';
			}
			return 'waiting';
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

<div
	class="flex min-h-screen flex-col items-center justify-center bg-linear-to-b from-slate-50 to-slate-100 px-4 py-16"
>
	<div class="w-full max-w-md text-center">
		{#if branding.logoUrl}
			<img src={branding.logoUrl} alt={branding.productName} class="mx-auto mb-6 h-12 w-auto" />
		{/if}

		<p class="mb-2 text-sm font-medium tracking-wide text-slate-500 uppercase">
			{branding.productName}
		</p>

		{#if view === 'checking'}
			<h1 class="mb-3 text-2xl font-semibold text-slate-900">Just a moment</h1>
			<p class="text-slate-600">Checking system status…</p>
		{:else if view === 'waiting'}
			<h1 class="mb-3 text-2xl font-semibold text-slate-900">Temporarily unavailable</h1>
			<p class="mb-2 text-slate-600">
				{branding.productName} is being updated and will be back shortly.
			</p>
			<p class="text-sm text-slate-500">
				This page will bring you back automatically when the update is finished. You don't need to do
				anything.
			</p>
		{:else if view === 'unavailable'}
			<h1 class="mb-3 text-2xl font-semibold text-slate-900">Temporarily unavailable</h1>
			<p class="mb-8 text-slate-600">
				We couldn't confirm the system status right now. We'll keep trying automatically.
			</p>
		{/if}

		{#if branding.supportUrl}
			<p class="mt-10 text-sm text-slate-500">
				Need help?
				<a
					href={branding.supportUrl}
					class="font-medium text-(--color-primary) underline-offset-2 hover:underline"
				>
					Contact support
				</a>
			</p>
		{/if}
	</div>
</div>
