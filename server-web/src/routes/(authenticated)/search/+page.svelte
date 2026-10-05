<script lang="ts">
	import { LocationController, RecordController, type LocationResponse, type RecordResponse } from '#lib/api/index.js';
	import { getApiClient } from '#lib/api-client.js';
	import MonoId from '#lib/components/MonoId.svelte';
	import TableCard from '#lib/components/TableCard.svelte';
	import PageContent from '#lib/components/layout/PageContent.svelte';
	import { t, tApiErrorResponse } from '#lib/i18n/catalog.js';
	import { DEFAULT_SEARCH_TYPE, locationKindQuery } from '#lib/search.js';

	let { data } = $props();

	type SearchItem = RecordResponse | LocationResponse;

	let additionalResults = $state<{
		key: string;
		items: SearchItem[];
		nextCursor?: string;
		error: string;
	}>();
	let loadingMore = $state(false);

	const searchKey = $derived(`${data.type ?? ''}:${data.q ?? ''}`);
	const items = $derived(additionalResults?.key === searchKey ? additionalResults.items : data.items);
	const nextCursor = $derived(additionalResults?.key === searchKey ? additionalResults.nextCursor : data.nextCursor);
	const loadMoreError = $derived(additionalResults?.key === searchKey ? additionalResults.error : '');
	const emptyKey = $derived(
		!data.type && !data.q
			? 'web.search.begin'
			: !data.q
				? 'web.search.enter_query'
				: !data.type
					? 'web.search.unsupported_type'
					: undefined,
	);

	const recordItems = $derived(data.type === 'record' ? (items as RecordResponse[]) : []);
	const locationItems = $derived(data.type !== 'record' ? (items as LocationResponse[]) : []);

	function recordTitle(record: RecordResponse): string {
		const title = record.properties['builtin:title'];
		return typeof title === 'string' && title.length > 0 ? title : t('web.common.em_dash');
	}

	function locationKindLabel(kind: LocationResponse['kind']): string {
		return kind === 'group' ? t('web.locations.kind_group') : t('web.locations.kind_user');
	}

	async function loadMore() {
		if (!nextCursor || !data.type || !data.q || loadingMore) {
			return;
		}

		loadingMore = true;

		try {
			const client = getApiClient();
			const result =
				data.type === 'record'
					? await RecordController.search({
							client,
							body: { q: data.q, cursor: nextCursor },
						})
					: await LocationController.searchLocations({
							client,
							body: { q: data.q, cursor: nextCursor },
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
</script>

<!-- TODO: rework this to be a dynamic table that allows the user to modify the visible columns -->
<PageContent>
	<h1 class="mb-2 text-2xl font-semibold">{t('web.search.title')}</h1>
	<p class="mb-6 text-hint">
		{data.q ? t(`web.search.summary_query.${data.type ?? DEFAULT_SEARCH_TYPE}`, data.q) : t('web.search.summary')}
	</p>

	{#if data.error}
		<section class="card p-5 text-sm text-destructive">{tApiErrorResponse(data.error)}</section>
	{:else if emptyKey}
		<section class="card p-5 text-hint">{t(emptyKey)}</section>
	{:else if data.type === 'record'}
		<TableCard
			title={t(`web.search.title.${data.type}`)}
			items={recordItems}
			empty={t(`web.search.empty.${data.type}`)}
			getKey={item => item.id}
		>
			{#snippet header()}
				<th class="px-5 py-3 font-medium">{t('web.search.col_title')}</th>
				<th class="px-5 py-3 font-medium">{t('web.search.col_type')}</th>
				<th class="px-5 py-3 font-medium">{t('web.search.col_id')}</th>
			{/snippet}
			{#snippet row(record)}
				<td class="px-5 py-4 font-medium">{recordTitle(record)}</td>
				<td class="px-5 py-4"><MonoId value={record.type} muted /></td>
				<td class="px-5 py-4"><MonoId value={record.id} muted /></td>
			{/snippet}
		</TableCard>
	{:else if data.type === 'user'}
		<TableCard
			title={t(`web.search.title.${data.type}`)}
			items={locationItems}
			empty={t(`web.search.empty.${data.type}`)}
			getKey={item => item.id}
		>
			{#snippet header()}
				<th class="px-5 py-3 font-medium">{t('web.search.col_name')}</th>
				<th class="px-5 py-3 font-medium">{t('web.search.col_username')}</th>
				<th class="px-5 py-3 font-medium">{t('web.search.col_enabled')}</th>
				<th class="px-5 py-3 font-medium">{t('web.search.col_id')}</th>
			{/snippet}
			{#snippet row(user)}
				<td class="px-5 py-4 font-medium">{user.displayName}</td>
				<td class="px-5 py-4"><MonoId value={user.name ?? ''} muted /></td>
				<td class="px-5 py-4">{user.enabled ? t('web.common.yes') : t('web.common.no')}</td>
				<td class="px-5 py-4"><MonoId value={user.id} muted /></td>
			{/snippet}
		</TableCard>
	{:else if data.type === 'location'}
		<TableCard
			title={t(`web.search.title.${data.type}`)}
			items={locationItems}
			empty={t(`web.search.empty.${data.type}`)}
			getKey={item => item.id}
		>
			{#snippet header()}
				<th class="px-5 py-3 font-medium">{t('web.search.col_name')}</th>
				<th class="px-5 py-3 font-medium">{t('web.search.col_kind')}</th>
				<th class="px-5 py-3 font-medium">{t('web.search.col_type')}</th>
				<th class="px-5 py-3 font-medium">{t('web.search.col_id')}</th>
			{/snippet}
			{#snippet row(location)}
				<td class="px-5 py-4 font-medium">{location.displayName}</td>
				<td class="px-5 py-4">{locationKindLabel(location.kind)}</td>
				<td class="px-5 py-4"><MonoId value={location.type} muted /></td>
				<td class="px-5 py-4"><MonoId value={location.id} muted /></td>
			{/snippet}
		</TableCard>
	{:else if data.type === 'group'}
		<TableCard
			title={t(`web.search.title.${data.type}`)}
			items={locationItems}
			empty={t(`web.search.empty.${data.type}`)}
			getKey={item => item.id}
		>
			{#snippet header()}
				<th class="px-5 py-3 font-medium">{t('web.search.col_name')}</th>
				<th class="px-5 py-3 font-medium">{t('web.search.col_type')}</th>
				<th class="px-5 py-3 font-medium">{t('web.search.col_id')}</th>
			{/snippet}
			{#snippet row(group)}
				<td class="px-5 py-4 font-medium">{group.displayName}</td>
				<td class="px-5 py-4"><MonoId value={group.type} muted /></td>
				<td class="px-5 py-4"><MonoId value={group.id} muted /></td>
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
