<script lang="ts" generics="T">
	import type { Snippet } from 'svelte';

	let {
		title,
		items,
		empty,
		getKey,
		header,
		row,
		class: className = '',
	}: {
		title: string;
		items: T[];
		empty: string;
		getKey: (item: T) => string | number;
		header: Snippet;
		row: Snippet<[T]>;
		class?: string;
	} = $props();
</script>

<section class={['card', className]}>
	<div class="card-header">
		<h2 class="text-lg font-medium">{title}</h2>
	</div>

	{#if items.length === 0}
		<p class="p-5 text-hint">{empty}</p>
	{:else}
		<div class="overflow-x-auto">
			<table class="w-full text-sm">
				<thead class="border-b border-border text-left text-label">
					<tr>
						{@render header()}
					</tr>
				</thead>
				<tbody class="divide-y divide-border">
					{#each items as item (getKey(item))}
						<tr>
							{@render row(item)}
						</tr>
					{/each}
				</tbody>
			</table>
		</div>
	{/if}
</section>
