<script lang="ts">
	import {
		RecordController,
		UserController,
		type RecordResponse,
		type UserResponse,
	} from '$lib/api';
	import { getApiClient } from '$lib/api-client';
	import MonoId from '$lib/components/MonoId.svelte';
	import TableCard from '$lib/components/TableCard.svelte';
	import PageContent from '$lib/components/layout/PageContent.svelte';

	let { data } = $props();

	type SearchItem = RecordResponse | UserResponse;

	let appended = $state<{
		key: string;
		items: SearchItem[];
		nextCursor: string | null;
		error: string;
	} | null>(null);
	let loadingMore = $state(false);

	const searchKey = $derived(`${data.type ?? ''}:${data.q ?? ''}`);
	const items = $derived(appended?.key === searchKey ? appended.items : data.items);
	const nextCursor = $derived(
		appended?.key === searchKey ? appended.nextCursor : data.nextCursor
	);
	const loadMoreError = $derived(appended?.key === searchKey ? appended.error : '');

	const typeLabel = $derived(data.type === 'user' ? 'users' : 'records');
	const summary = $derived(data.q ? `Searching ${typeLabel} for “${data.q}”` : 'Search');

	const recordItems = $derived(data.type === 'record' ? (items as RecordResponse[]) : []);
	const userItems = $derived(data.type === 'user' ? (items as UserResponse[]) : []);

	function recordTitle(record: RecordResponse): string {
		const title = record.properties?.['builtin:title'];
		return typeof title === 'string' && title.length > 0 ? title : '—';
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
					? await RecordController.search1({
							client,
							body: { q: data.q, cursor: nextCursor },
						})
					: await UserController.search({
							client,
							body: { q: data.q, cursor: nextCursor },
						});

			const page = result.data?.success ? result.data.data : null;
			if (result.error || !page) {
				appended = {
					key: searchKey,
					items: [...items],
					nextCursor,
					error:
						typeof result.error?.error === 'string'
							? result.error.error
							: 'Failed to load more results.',
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

<PageContent>
	<h1 class="mb-2 text-2xl font-semibold">Search</h1>
	<p class="mb-6 text-hint">{summary}</p>

	{#if data.error}
		<section class="card p-5 text-sm text-destructive">{data.error}</section>
	{:else if data.message}
		<section class="card p-5 text-hint">{data.message}</section>
	{:else if data.type === 'record'}
		<TableCard
			title="Records"
			items={recordItems}
			empty="No records matched your search."
			getKey={item => item.id}
		>
			{#snippet header()}
				<th class="px-5 py-3 font-medium">Title</th>
				<th class="px-5 py-3 font-medium">Type</th>
				<th class="px-5 py-3 font-medium">Id</th>
			{/snippet}
			{#snippet row(record)}
				<td class="px-5 py-4 font-medium">{recordTitle(record)}</td>
				<td class="px-5 py-4"><MonoId value={record.type} muted /></td>
				<td class="px-5 py-4"><MonoId value={record.id} muted /></td>
			{/snippet}
		</TableCard>
	{:else if data.type === 'user'}
		<TableCard
			title="Users"
			items={userItems}
			empty="No users matched your search."
			getKey={item => item.id}
		>
			{#snippet header()}
				<th class="px-5 py-3 font-medium">Username</th>
				<th class="px-5 py-3 font-medium">Enabled</th>
				<th class="px-5 py-3 font-medium">Id</th>
			{/snippet}
			{#snippet row(user)}
				<td class="px-5 py-4 font-medium">{user.username}</td>
				<td class="px-5 py-4">{user.enabled ? 'Yes' : 'No'}</td>
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
				{loadingMore ? 'Loading…' : 'Load more'}
			</button>
		</div>
	{/if}
</PageContent>
