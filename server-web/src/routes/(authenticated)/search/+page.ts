import { RecordController, UserController, type RecordResponse, type UserResponse } from '$lib/api';
import { getApiClient } from '$lib/api-client';
import { t } from '$lib/i18n/catalog';

export type SearchType = 'record' | 'user';

function isSearchType(value: string | null): value is SearchType {
	return value === 'record' || value === 'user';
}

function searchError(error: { error?: unknown } | undefined): string | null {
	if (typeof error?.error === 'string') {
		return error.error;
	}
	if (error) {
		return t('web.search.failed');
	}
	return null;
}

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
			error: null as string | null,
			message:
				!type && !q
					? t('web.search.begin')
					: !q
						? t('web.search.enter_query')
						: t('web.search.unsupported_type'),
		};
	}

	const client = getApiClient();
	const result =
		type === 'record'
			? await RecordController.search1({ client, body: { q } })
			: await UserController.search({ client, body: { q } });

	const error = searchError(result.error);
	const payload = result.data?.success ? result.data.data : null;

	return {
		type,
		q,
		items: payload?.items ?? [],
		nextCursor: payload?.nextCursor ?? null,
		error,
		message: null as string | null,
	};
}
