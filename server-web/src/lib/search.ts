import { browser } from '$app/env';

export const SEARCH_TYPES = ['record', 'location', 'user', 'group'] as const;

export type SearchType = (typeof SEARCH_TYPES)[number];

export const DEFAULT_SEARCH_TYPE: SearchType = 'record';

const COLUMNS_STORAGE_KEY = 'orm.search.columns';

const DEFAULT_COLUMNS: Record<SearchType, readonly string[]> = {
	record: ['builtin:title', 'builtin:keywords', 'builtin:notes'],
	location: ['builtin:name'],
	user: ['builtin:name', 'builtin:given_name', 'builtin:surname', 'builtin:email'],
	group: ['builtin:name'],
};

type StoredColumnLayouts = Partial<Record<SearchType, string[]>>;

export function isSearchType(value: string | null | undefined): value is SearchType {
	return value != null && (SEARCH_TYPES as readonly string[]).includes(value);
}

export function locationKindQuery(type: Exclude<SearchType, 'record'>): { kind?: 'user' | 'group' } {
	if (type === 'user' || type === 'group') {
		return { kind: type };
	}
	return {};
}

function readStoredLayouts(): StoredColumnLayouts {
	if (!browser) {
		return {};
	}
	try {
		const raw = localStorage.getItem(COLUMNS_STORAGE_KEY);
		if (!raw) {
			return {};
		}
		const parsed = JSON.parse(raw) as unknown;
		if (parsed == null || typeof parsed !== 'object' || Array.isArray(parsed)) {
			return {};
		}
		return parsed as StoredColumnLayouts;
	} catch {
		return {};
	}
}

function isColumnList(value: unknown): value is string[] {
	return Array.isArray(value) && value.every(entry => typeof entry === 'string' && entry.length > 0);
}

/** Last chosen display columns for a search type, if any. */
export function getStoredSearchColumns(type: SearchType): string[] | undefined {
	const stored = readStoredLayouts()[type];
	return isColumnList(stored) ? [...stored] : undefined;
}

/** Persist the display-column layout for a search type. */
export function setStoredSearchColumns(type: SearchType, columns: string[]): void {
	if (!browser || !isColumnList(columns)) {
		return;
	}
	const next: StoredColumnLayouts = {
		...readStoredLayouts(),
		[type]: [...columns],
	};
	localStorage.setItem(COLUMNS_STORAGE_KEY, JSON.stringify(next));
}

/** Stored layout for the type, otherwise the builtin default columns. */
export function resolveSearchColumns(type: SearchType): string[] {
	return getStoredSearchColumns(type) ?? [...DEFAULT_COLUMNS[type]];
}
