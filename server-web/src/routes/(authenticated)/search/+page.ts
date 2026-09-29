import { RecordController, UserController, type RecordResponse, type UserResponse } from '$lib/api';
import { getApiClient } from '$lib/api-client';

export type SearchType = 'record' | 'user';

function isSearchType(value: string | null): value is SearchType {
	return value === 'record' || value === 'user';
}

export type SearchEmptyKey = 'web.search.begin' | 'web.search.enter_query' | 'web.search.unsupported_type';

export async function load({ parent, url }: { parent: () => Promise<unknown>; url: URL }) {
	await parent();

	const typeParam = url.searchParams.get('type');
	const q = url.searchParams.get('q')?.trim() ?? '';
	const type = isSearchType(typeParam) ? typeParam : null;

	if (!type || !q) {
		return {
			type,
			q,
			items: [] as Array<RecordResponse | UserResponse>,
			nextCursor: null as string | null,
			emptyKey: (!type && !q
				? 'web.search.begin'
				: !q
					? 'web.search.enter_query'
					: 'web.search.unsupported_type') as SearchEmptyKey | null,
		};
	}

	const client = getApiClient();
	const result =
		type === 'record'
			? await RecordController.search1({ client, body: { q } })
			: await UserController.search({ client, body: { q } });

	const payload = result.data?.success ? result.data.data : null;

	return {
		type,
		q,
		items: payload?.items ?? [],
		nextCursor: payload?.nextCursor,
		error: result.error,
		emptyKey: null as SearchEmptyKey | null,
	};
}
