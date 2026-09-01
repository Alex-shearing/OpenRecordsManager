<script lang="ts">
	import { DatabaseController } from '$lib/api';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';

	let view = $state<'checking' | 'waiting' | 'unavailable' | 'ready'>('checking');

	const pollIntervalMs = 3000;

	async function checkStatus() {
		try {
			const { data, error } = await DatabaseController.status();
			if (error) {
				return 'unavailable';
			}
			if (data.data.state === 'READY') {
				await goto(page.url.searchParams.get('redirect') || '');
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

<div class="card text-center">
	<div class="card-body">
		{#if view === 'checking'}
			<h1 class="mb-3 text-2xl font-semibold">Just a moment</h1>
			<p class="text-hint">Checking system status…</p>
		{:else if view === 'waiting'}
			<h1 class="mb-3 text-2xl font-semibold">Temporarily unavailable</h1>
			<p class="mb-2 text-muted-foreground">The application is being updated and will be back shortly.</p>
			<p class="text-hint">
				This page will bring you back automatically when the update is finished. You don't need to do anything.
			</p>
		{:else if view === 'unavailable'}
			<h1 class="mb-3 text-2xl font-semibold">Temporarily unavailable</h1>
			<p class="text-muted-foreground">
				We couldn't confirm the system status right now. We'll keep trying automatically.
			</p>
		{/if}
	</div>
</div>
