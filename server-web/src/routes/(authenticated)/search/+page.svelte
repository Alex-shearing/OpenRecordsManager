<script lang="ts">
	import {
		LocationController,
		RecordController,
		type LocationResponse,
		type RecordResponse,
		type SimpleObjectPropertyResponse,
	} from '#lib/api/index.js';
	import { getApiClient } from '#lib/api-client.js';
	import AppDialog from '#lib/components/AppDialog.svelte';
	import MonoId from '#lib/components/MonoId.svelte';
	import TableCard from '#lib/components/TableCard.svelte';
	import TransferList from '#lib/components/TransferList.svelte';
	import PageContent from '#lib/components/layout/PageContent.svelte';
	import { t, tApiErrorResponse } from '#lib/i18n/catalog.js';
	import { objectPropertyName } from '#lib/i18n/labels.js';
	import { formatObjectPropertyValue } from '#lib/properties/formatValue.js';
	import { DEFAULT_SEARCH_TYPE, locationKindQuery, setStoredSearchColumns } from '#lib/search.js';
	import ColumnsIcon from 'phosphor-svelte/lib/ColumnsIcon';
	import { SvelteMap } from 'svelte/reactivity';

	let { data } = $props();

	let additionalResults = $state<{
		key: string;
		items: Array<RecordResponse | LocationResponse>;
		nextCursor?: string;
		error: string;
	}>();
	let columnLayout = $state<{ key: string; columns: string[] }>();
	let loadingMore = $state(false);
	let columnsDialogOpen = $state(false);
	let draftSelected = $state<string[]>([]);

	const searchKey = $derived(`${data.type ?? ''}:${data.q ?? ''}`);
	const items = $derived(additionalResults?.key === searchKey ? additionalResults.items : data.items);
	const nextCursor = $derived(additionalResults?.key === searchKey ? additionalResults.nextCursor : data.nextCursor);
	const columns = $derived(
		columnLayout?.key === searchKey ? columnLayout.columns : (data.columns ?? []),
	);
	const loadMoreError = $derived(additionalResults?.key === searchKey ? additionalResults.error : '');
	const emptyKey = $derived(
		!data.type && !data.q
			? 'web.search.begin'
			: !data.q
				? 'web.search.enter_query'
				: !data.type
					? 'web.search.unsupported_type'
					: undefined
	);

	const definitionById = $derived(new Map((data.properties ?? []).map(property => [property.id, property] as const)));

	const columnItems = $derived.by(() => {
		const byId = new SvelteMap<string, SimpleObjectPropertyResponse>(definitionById);
		for (const id of [...columns, ...draftSelected]) {
			if (!byId.has(id)) {
				byId.set(id, { id, type: '' });
			}
		}
		return [...byId.values()];
	});

	function openColumnsDialog() {
		draftSelected = [...columns];
		columnsDialogOpen = true;
	}

	async function loadMore() {
		if (!nextCursor || !data.type || !data.q || loadingMore) {
			return;
		}

		loadingMore = true;

		try {
			const search = {
				client: getApiClient(),
				body: {
					q: data.q,
					cursor: nextCursor,
				},
			};
			const result =
				data.type === 'record'
					? await RecordController.search(search)
					: await LocationController.searchLocations({
							...search,
							query: locationKindQuery(data.type),
						});

			const page = result.data?.success ? result.data.data : null;
			if (result.error || !page) {
				additionalResults = {
					key: searchKey,
					items: [...items],
					nextCursor,
					error: result.error ? tApiErrorResponse(result.error) : t('web.search.load_more_failed'),
				};
				return;
			}

			additionalResults = {
				key: searchKey,
				items: [...items, ...(page.items ?? [])],
				nextCursor: page.nextCursor,
				error: '',
			};
		} finally {
			loadingMore = false;
		}
	}

	function applyColumns() {
		columnsDialogOpen = false;
		if (data.type) {
			setStoredSearchColumns(data.type, draftSelected);
		}
		columnLayout = { key: searchKey, columns: [...draftSelected] };
	}
</script>

<PageContent>
	<h1 class="mb-2 text-2xl font-semibold">{t('web.search.title')}</h1>
	<p class="mb-6 text-hint">
		{data.q ? t(`web.search.summary_query.${data.type ?? DEFAULT_SEARCH_TYPE}`, data.q) : t('web.search.summary')}
	</p>

	{#if data.error}
		<section class="card p-5 text-sm text-destructive">{tApiErrorResponse(data.error)}</section>
	{:else if emptyKey}
		<section class="card p-5 text-hint">{t(emptyKey)}</section>
	{:else if data.type}
		<TableCard
			title={t(`web.search.title.${data.type}`)}
			{items}
			empty={t(`web.search.empty.${data.type}`)}
			getKey={item => item.id}
		>
			{#snippet actions()}
				<button
					type="button"
					class="btn-ghost inline-flex items-center gap-1.5"
					aria-label={t('web.search.columns')}
					onclick={openColumnsDialog}
				>
					<ColumnsIcon class="size-4" aria-hidden="true" />
					{t('web.search.columns')}
				</button>
			{/snippet}
			{#snippet header()}
				{#each columns as column, i (column)}
					<th class="px-5 py-3" class:font-medium={i === 0}>{objectPropertyName(column)}</th>
				{/each}
			{/snippet}
			{#snippet row(item)}
				{#each columns as column, i (column)}
					<td class="px-5 py-4" class:font-medium={i === 0}
						>{formatObjectPropertyValue(
							definitionById.get(column),
							item.properties?.[column],
							t('web.common.em_dash')
						)}</td
					>
				{/each}
			{/snippet}
		</TableCard>
	{/if}

	{#if nextCursor}
		<div class="mt-4 flex flex-col items-start gap-2">
			{#if loadMoreError}
				<p class="text-sm text-destructive">{loadMoreError}</p>
			{/if}
			<button type="button" class="btn-secondary" disabled={loadingMore} onclick={loadMore}>
				{loadingMore ? t('web.common.loading') : t('web.search.load_more')}
			</button>
		</div>
	{/if}
</PageContent>

<AppDialog bind:open={columnsDialogOpen} size="wide" title="web.search.columns_title">
	{#snippet body()}
		<TransferList
			items={columnItems}
			bind:selected={draftSelected}
			getKey={p => p.id}
			getSearchText={p => `${objectPropertyName(p.id)} ${p.id}`}
		>
			{#snippet item(property)}
				<span class="font-medium">{objectPropertyName(property.id)}</span>
				<span class="block"><MonoId value={property.id} muted /></span>
			{/snippet}
		</TransferList>
	{/snippet}
	{#snippet footer()}
		<div class="flex flex-wrap justify-end gap-2">
			<button type="button" class="btn-secondary" onclick={() => (columnsDialogOpen = false)}>
				{t('web.common.cancel')}
			</button>
			<button type="button" class="btn-primary" onclick={applyColumns}>
				{t('web.search.columns_apply')}
			</button>
		</div>
	{/snippet}
</AppDialog>
