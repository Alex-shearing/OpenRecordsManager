<script lang="ts">
	import { getClient } from '$lib';

	const types = getClient().GET('/api/templates');

	let detail = $state<{ type: string; template: string; body: unknown } | null>(null);
	let detailError = $state('');

	const registerTemplate = async (type: string, template: string) => {
		const { error } = await getClient().POST(`/api/templates/{type}/{template}/register`, {
			params: {
				path: { type, template },
				query: { includeDependencies: true }
			}
		});

		if (error) {
			alert(`Failed to register template ${template}: ${error.error}`);
		} else {
			alert(`Template ${template} registered successfully!`);
		}
	};

	async function loadDetail(type: string, template: string) {
		detailError = '';
		const { data, error } = await getClient().GET('/api/templates/{type}/{template}', {
			params: { path: { type, template } }
		});
		if (error) {
			detailError = error.error ?? 'Failed to load template';
			detail = null;
			return;
		}
		detail = { type, template, body: data?.data ?? null };
	}
</script>

<div class="flex flex-col gap-4">
	<h1 class="text-2xl font-semibold">Templates</h1>
	{#await types}
		<p class="text-sm text-gray-500">Loading…</p>
	{:then { data, error }}
		{#if data?.data}
			{#each data.data as type (type)}
				<section class="rounded-lg border border-gray-200 p-4 dark:border-gray-700">
					<h2 class="mb-3 text-lg font-medium">{type}</h2>
					{#await getClient().GET(`/api/templates/{type}`, { params: { path: { type } } })}
						<p class="text-sm text-gray-500">Loading…</p>
					{:then { data: templatesData }}
						{#if templatesData?.data}
							<ul class="flex flex-wrap gap-2">
								{#each templatesData.data as template (template)}
									<li class="flex gap-1">
										<button
											type="button"
											class="rounded bg-blue-500 px-3 py-1.5 text-sm font-medium text-white hover:bg-blue-700"
											onclick={() => registerTemplate(type, template)}
										>
											Register {template}
										</button>
										<button
											type="button"
											class="rounded border border-gray-300 px-3 py-1.5 text-sm"
											onclick={() => loadDetail(type, template)}
										>
											Details
										</button>
									</li>
								{/each}
							</ul>
						{/if}
					{/await}
				</section>
			{/each}
		{:else}
			<p class="text-sm text-red-600">Failed to load templates {error?.error}</p>
		{/if}
	{/await}

	{#if detailError}
		<p class="text-sm text-red-600">{detailError}</p>
	{/if}
	{#if detail}
		<section class="rounded-lg border border-gray-200 p-4 dark:border-gray-700">
			<h2 class="mb-2 text-lg font-medium">
				{detail.type} / {detail.template}
			</h2>
			<pre class="overflow-auto text-xs">{JSON.stringify(detail.body, null, 2)}</pre>
		</section>
	{/if}
</div>
