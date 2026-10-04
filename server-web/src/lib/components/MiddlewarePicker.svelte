<script lang="ts">
	import type { SimpleMiddlewareResponse } from '$lib/api/types.gen';
	import MonoId from '$lib/components/MonoId.svelte';
	import TransferList from '$lib/components/TransferList.svelte';
	import { t } from '$lib/i18n/catalog';

	let {
		middlewares,
		selected = $bindable<string[]>([]),
		disabled = false,
		showEmpty = false,
	}: {
		middlewares: SimpleMiddlewareResponse[];
		selected?: string[];
		disabled?: boolean;
		showEmpty?: boolean;
	} = $props();

	const labelId = $props.id();

	function compareAvailable(a: SimpleMiddlewareResponse, b: SimpleMiddlewareResponse) {
		const nameCmp = a.name.localeCompare(b.name);
		if (nameCmp !== 0) return nameCmp;
		const typeCmp = a.type.localeCompare(b.type);
		return typeCmp !== 0 ? typeCmp : a.id.localeCompare(b.id);
	}
</script>

{#if middlewares.length === 0}
	{#if showEmpty}
		<div role="group" aria-labelledby={labelId}>
			<p id={labelId} class="text-label">{t('web.middlewares.label')}</p>
			<p class="text-hint">{t('web.middlewares.none_available')}</p>
		</div>
	{/if}
{:else}
	<div class="flex flex-col gap-2">
		<p id={labelId} class="text-label">{t('web.middlewares.label')}</p>
		<TransferList
			items={middlewares}
			bind:selected
			getKey={middleware => middleware.id}
			getSearchText={m => `${m.name} ${m.type} ${m.id}`}
			{disabled}
			labelledBy={labelId}
			selectedTitle="web.common.enabled"
			{compareAvailable}
		>
			{#snippet item(middleware)}
				<span class="font-medium">{middleware.name}</span>
				<span class="block text-hint">{middleware.type}</span>
				<span class="block"><MonoId value={middleware.id} muted /></span>
			{/snippet}
		</TransferList>
	</div>
{/if}
