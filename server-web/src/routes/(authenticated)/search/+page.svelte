<script lang="ts">
	import { LocationController, RecordController, type LocationResponse, type RecordResponse } from '#lib/api/index.js';
	import { getApiClient } from '#lib/api-client.js';
	import MonoId from '#lib/components/MonoId.svelte';
	import TableCard from '#lib/components/TableCard.svelte';
	import PageContent from '#lib/components/layout/PageContent.svelte';
	import { t, tApiErrorResponse } from '#lib/i18n/catalog.js';

	let { data } = $props();

	type SearchItem = RecordResponse | LocationResponse;

	let appended = $state<{
		key: string;
		items: SearchItem[];
		nextCursor: string | null;
		error: string;
	} | null>(null);
	let loadingMore = $state(false);

	const searchKey = $derived(`${data.type ?? ''}:${data.q ?? ''}`);
	const items = $derived(appended?.key === searchKey ? appended.items : data.items);
	const nextCursor = $derived(appended?.key === searchKey ? appended.nextCursor : data.nextCursor);
	const loadMoreError = $derived(appended?.key === searchKey ? appended.error : '');

	const typeLabel = $derived(data.type === 'user' ? t('web.search.users_label') : t('web.search.records_label'));
	const summary = $derived(data.q ? t('web.search.summary_query', typeLabel, data.q) : t('web.search.summary'));

	const recordItems = $derived(data.type === 'record' ? (items as RecordResponse[]) : []);
	const userItems = $derived(data.type === 'user' ? (items as LocationResponse[]) : []);

	function recordTitle(record: RecordResponse): string {
		const title = record.properties?.['builtin:title'];
		return typeof title === 'string' && title.length > 0 ? title : t('web.common.em_dash');
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
							body: { q: data.q, cursor: nextCursor, kind: 'user' },
						});

			const page = result.data?.success ? result.data.data : null;
			if (result.error || !page) {
				appended = {
					key: searchKey,
					items: [...items],
					nextCursor,
					error: result.error ? tApiErrorResponse(result.error) : t('web.search.load_more_failed'),
				};
				return;
			}

			appended = {
				key: searchKey,
				items: [...items, ...(page.items ?? [])],
				nextCursor: page.nextCursor ?? null,
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
	<p class="mb-6 text-hint">{summary}</p>

	{#if data.error}
		<section class="card p-5 text-sm text-destructive">{tApiErrorResponse(data.error)}</section>
	{:else if data.emptyKey}
		<section class="card p-5 text-hint">{t(data.emptyKey)}</section>
	{:else if data.type === 'record'}
		<TableCard
			title={t('web.search.records_title')}
			items={recordItems}
			empty={t('web.search.empty_records')}
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
			title={t('web.search.users_title')}
			items={userItems}
			empty={t('web.search.empty_users')}
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
