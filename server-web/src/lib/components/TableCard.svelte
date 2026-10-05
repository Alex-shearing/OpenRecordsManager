<script lang="ts" generics="T">
	import type { Snippet } from 'svelte';

	let {
		title,
		items,
		empty,
		getKey,
		getHref,
		header,
		row,
		actions,
		class: className = '',
	}: {
		title: string;
		items: T[];
		empty: string;
		getKey: (item: T) => string | number;
		getHref?: (item: T) => string;
		header: Snippet;
		row: Snippet<[T]>;
		actions?: Snippet;
		class?: string;
	} = $props();
</script>

<section class={['card', className]}>
	<div class="card-header flex items-center justify-between gap-3">
		<h2 class="text-lg font-medium">{title}</h2>
		{#if actions}
			<div class="shrink-0">{@render actions()}</div>
		{/if}
	</div>

	{#if items.length === 0}
		<p class="p-5 text-hint">{empty}</p>
	{:else}
		<div class="overflow-x-auto">
			<table class="w-full text-sm">
				<thead class="border-b border-border text-left text-label">
					<tr>
						{@render header()}
						{#if getHref}
							<th class="w-0 p-0" aria-hidden="true"></th>
						{/if}
					</tr>
				</thead>
				<tbody class="divide-y divide-border">
					{#each items as item (getKey(item))}
						{@const href = getHref?.(item)}
						<tr
							class={
								href
									? 'relative cursor-pointer transition-colors hover:bg-surface-hover focus-within:bg-surface-hover'
									: undefined
							}
						>
							{@render row(item)}
							{#if href}
								<td class="w-0 p-0">
									<a
										href={href}
										class="absolute inset-0 focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-primary"
										aria-label={String(getKey(item))}
									></a>
								</td>
							{/if}
						</tr>
					{/each}
				</tbody>
			</table>
		</div>
	{/if}
</section>
