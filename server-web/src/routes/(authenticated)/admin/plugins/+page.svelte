<script lang="ts">
	import { getClient } from '$lib';

	const promise = getClient().GET('/api/plugins');
</script>

<div class="flex flex-col gap-4">
	<h1 class="text-2xl font-semibold">Plugins</h1>
	<p class="text-sm text-gray-600">Enabled plugins on this server (read-only).</p>

	{#await promise}
		<p class="text-sm text-gray-500">Loading…</p>
	{:then { data, error }}
		{#if error || !data?.data}
			<p class="text-sm text-red-600">{error?.error ?? 'Failed to load plugins'}</p>
		{:else if data.data.length === 0}
			<p class="text-sm text-gray-500">No plugins enabled</p>
		{:else}
			<ul class="divide-y divide-gray-200 rounded border border-gray-200 dark:border-gray-700">
				{#each data.data as name (name)}
					<li class="px-4 py-3 font-mono text-sm">{name}</li>
				{/each}
			</ul>
		{/if}
	{/await}

	<p class="text-sm">
		<a href="/admin/plugins/templates" class="text-(--color-primary) underline">Manage templates</a>
	</p>
</div>
