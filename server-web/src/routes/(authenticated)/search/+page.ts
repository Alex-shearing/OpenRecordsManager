import { LocationController, RecordController, type LocationResponse, type RecordResponse } from '#lib/api/index.js';
import { getApiClient } from '#lib/api-client.js';
import { isSearchType, locationKindQuery, resolveSearchColumns } from '#lib/search.js';

export async function load({ parent, url }) {
	await parent();

	const typeParam = url.searchParams.get('type');
	const q = url.searchParams.get('q')?.trim() ?? '';
	const type = isSearchType(typeParam) ? typeParam : undefined;

	if (!type || !q) {
		return {
			type,
			q,
			items: [] as Array<RecordResponse | LocationResponse>,
			columns: [] as string[],
		};
	}

	const result =
		type === 'record'
			? await RecordController.search({ client: getApiClient(), body: { q } })
			: await LocationController.searchLocations({
					client: getApiClient(),
					body: { q },
					query: locationKindQuery(type),
				});

	return {
		type,
		q,
		items: result.data?.data?.items ?? [],
		nextCursor: result.data?.data?.nextCursor,
		columns: resolveSearchColumns(type),
		error: result.error,
	};
}
