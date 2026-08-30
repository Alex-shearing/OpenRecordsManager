<script lang="ts">
	import { getClient } from '$lib';

	const promise = getClient().GET('/api/record_types');
</script>

<div class="flex flex-col gap-4">
	<div class="flex items-center justify-between gap-4">
		<h1 class="text-2xl font-semibold">Record types</h1>
		<a href="/records/new" class="text-sm text-(--color-primary) underline">Create record</a>
	</div>

	{#await promise}
		<p class="text-sm text-gray-500">Loading…</p>
	{:then { data, error }}
		{#if error || !data?.data}
			<p class="text-sm text-red-600">{error?.error ?? 'Failed to load record types'}</p>
		{:else if data.data.length === 0}
			<p class="text-sm text-gray-500">No record types registered. Register templates from Admin.</p>
		{:else}
			<ul class="divide-y divide-gray-200 rounded border border-gray-200 dark:divide-gray-700 dark:border-gray-700">
				{#each data.data as typeId (typeId)}
					<li>
						<a
							href="/record-types/{encodeURIComponent(typeId)}"
							class="block px-4 py-3 text-sm hover:bg-gray-50 dark:hover:bg-gray-900"
						>
							{typeId}
						</a>
					</li>
				{/each}
			</ul>
		{/if}
	{/await}
</div>
