<script lang="ts">
	import { page } from '$app/state';
	import { getClient } from '$lib';
	import { formatValue } from '$lib/api';

	let typeId = $derived(decodeURIComponent(page.params.id ?? ''));

	let promise = $derived(
		getClient().GET('/api/record_types/{id}', {
			params: { path: { id: typeId } }
		})
	);
</script>

<div class="flex flex-col gap-4">
	<p class="text-sm">
		<a href="/record-types" class="text-(--color-primary) underline">Record types</a>
	</p>

	{#await promise}
		<p class="text-sm text-gray-500">Loading…</p>
	{:then { data, error }}
		{#if error || !data?.data}
			<p class="text-sm text-red-600">{error?.error ?? 'Failed to load type'}</p>
		{:else}
			{@const t = data.data}
			<h1 class="text-2xl font-semibold">{t.name}</h1>
			<p class="text-sm text-gray-600">{t.description}</p>
			<dl class="grid gap-2 text-sm sm:grid-cols-2">
				<div>
					<dt class="text-gray-500">ID</dt>
					<dd class="font-mono text-xs">{t.id}</dd>
				</div>
				<div>
					<dt class="text-gray-500">Security filter usage</dt>
					<dd>{t.securityFilterUsage}</dd>
				</div>
				{#if t.securityFilter}
					<div>
						<dt class="text-gray-500">Security filter</dt>
						<dd class="font-mono text-xs">{t.securityFilter}</dd>
					</div>
				{/if}
				{#if t.contentTypes?.length}
					<div>
						<dt class="text-gray-500">Content types</dt>
						<dd>{t.contentTypes.join(', ')}</dd>
					</div>
				{/if}
			</dl>

			<h2 class="mt-4 text-lg font-medium">Properties</h2>
			{#if t.properties.length === 0}
				<p class="text-sm text-gray-500">No properties</p>
			{:else}
				<ul class="divide-y divide-gray-200 rounded border border-gray-200 text-sm dark:border-gray-700">
					{#each t.properties as prop (prop.property.id)}
						<li class="px-4 py-3">
							<div class="font-medium">{prop.property.name}</div>
							<div class="text-xs text-gray-500">
								{prop.property.id} · {prop.property.type}
								{#if prop.default !== undefined}
									· default: {formatValue(prop.default)}
								{/if}
							</div>
							{#if prop.property.description}
								<p class="mt-1 text-gray-600">{prop.property.description}</p>
							{/if}
						</li>
					{/each}
				</ul>
			{/if}

			<a
				href="/records/new?type={encodeURIComponent(t.id)}"
				class="mt-2 inline-block text-sm text-(--color-primary) underline"
			>
				Create {t.name} record
			</a>
		{/if}
	{/await}
</div>
