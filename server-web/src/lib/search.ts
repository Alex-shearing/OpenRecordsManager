export const SEARCH_TYPES = ['record', 'location', 'user', 'group'] as const;

export type SearchType = (typeof SEARCH_TYPES)[number];

export const DEFAULT_SEARCH_TYPE: SearchType = 'record';

export function isSearchType(value: string | null | undefined): value is SearchType {
	return value != null && (SEARCH_TYPES as readonly string[]).includes(value);
}

export function locationKindQuery(type: Exclude<SearchType, 'record'>): { kind?: 'user' | 'group' } {
	if (type === 'user' || type === 'group') {
		return { kind: type };
	}
	return {};
}
