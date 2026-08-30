<script lang="ts">
	import { getClient } from '$lib';
	import { formatValue } from '$lib/api';
	import ActionRunner from '$lib/components/ActionRunner.svelte';
	import type { components } from '$lib/types/schema';

	type UserResponse = components['schemas']['UserResponse'];

	let user = $state<UserResponse | null>(null);
	let error = $state('');
	let loading = $state(true);

	function unwrapMe(data: unknown): UserResponse | null {
		if (!data || typeof data !== 'object') return null;
		const record = data as Record<string, unknown>;
		if (record.data && typeof record.data === 'object' && record.data !== null && 'username' in record.data) {
			return record.data as UserResponse;
		}
		if ('username' in record && 'id' in record) return record as UserResponse;
		return null;
	}

	$effect(() => {
		loading = true;
		getClient()
			.GET('/api/user/me')
			.then(({ data, error: err }) => {
				loading = false;
				const me = unwrapMe(data);
				if (err || !me) {
					error = err?.error ?? 'Failed to load profile';
					user = null;
					return;
				}
				user = me;
				error = '';
			});
	});
</script>

<div class="flex flex-col gap-6">
	<h1 class="text-2xl font-semibold">Profile</h1>

	{#if loading}
		<p class="text-sm text-gray-500">Loading…</p>
	{:else if error}
		<p class="text-sm text-red-600">{error}</p>
	{:else if user}
		<section class="rounded-lg border border-gray-200 p-4 dark:border-gray-700">
			<dl class="grid gap-2 text-sm sm:grid-cols-2">
				<div>
					<dt class="text-gray-500">Username</dt>
					<dd class="font-medium">{user.username}</dd>
				</div>
				<div>
					<dt class="text-gray-500">User ID</dt>
					<dd class="font-mono text-xs">{user.id}</dd>
				</div>
			</dl>

			{#if user.properties && Object.keys(user.properties).length}
				<h2 class="mt-4 mb-2 font-medium">Properties</h2>
				<ul class="space-y-1 text-sm">
					{#each Object.entries(user.properties) as [key, val] (key)}
						<li>
							<span class="text-gray-500">{key}:</span>
							{formatValue(val)}
						</li>
					{/each}
				</ul>
			{/if}
		</section>

		<ActionRunner kind="user" targetId={user.id} />
	{/if}
</div>
