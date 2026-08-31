<script lang="ts">
	import { DatabaseController } from '$lib';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';

	let { data } = $props();
	let view = $state<'checking' | 'waiting' | 'unavailable' | 'ready'>('checking');

	const pollIntervalMs = 3000;

	function returnPath() {
		const redirect = page.url.searchParams.get('redirect');
		if (redirect && redirect.startsWith('/') && !redirect.startsWith('//')) {
			return redirect;
		}
		return '/';
	}

	async function checkStatus() {
		try {
			const { data, error } = await DatabaseController.status();
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

<div class="flex min-h-screen flex-col items-center justify-center bg-surface-hover px-4 py-16">
	<div class="w-full max-w-md text-center">
		{#if data.branding.logoUrl}
			<img src={data.branding.logoUrl} alt={data.branding.productName} class="mx-auto mb-6 h-12 w-auto" />
		{/if}

		<p class="mb-2 text-sm font-medium tracking-wide text-muted-foreground uppercase">
			{data.branding.productName}
		</p>

		{#if view === 'checking'}
			<h1 class="mb-3 text-2xl font-semibold text-foreground">Just a moment</h1>
			<p class="text-muted-foreground">Checking system status…</p>
		{:else if view === 'waiting'}
			<h1 class="mb-3 text-2xl font-semibold text-foreground">Temporarily unavailable</h1>
			<p class="mb-2 text-muted-foreground">
				{data.branding.productName} is being updated and will be back shortly.
			</p>
			<p class="text-hint">
				This page will bring you back automatically when the update is finished. You don't need to do
				anything.
			</p>
		{:else if view === 'unavailable'}
			<h1 class="mb-3 text-2xl font-semibold text-foreground">Temporarily unavailable</h1>
			<p class="mb-8 text-muted-foreground">
				We couldn't confirm the system status right now. We'll keep trying automatically.
			</p>
		{/if}

		{#if data.branding.supportUrl}
			<p class="mt-10 text-hint">
				Need help?
				<a href={data.branding.supportUrl} class="text-link">Contact support</a>
			</p>
		{/if}
	</div>
</div>
