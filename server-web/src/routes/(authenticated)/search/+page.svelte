<script lang="ts">
	import PageContent from '$lib/components/layout/PageContent.svelte';

	let { data } = $props();
</script>

<PageContent>
	<h1 class="mb-6 text-2xl font-semibold">Search</h1>

	<section class="card p-4">
		<h2 class="text-lg font-medium">Search parameters</h2>

		{#if !data.type && !data.q}
			<p class="mt-4 text-hint">No search parameters provided.</p>
		{:else}
			<dl class="mt-4 grid gap-3 sm:grid-cols-2">
				<div>
					<dt class="text-hint">type</dt>
					<dd class="font-medium">{data.type ?? '—'}</dd>
				</div>
				<div>
					<dt class="text-hint">q</dt>
					<dd class="font-medium">{data.q ?? '—'}</dd>
				</div>
			</dl>
		{/if}
	</section>
</PageContent>
