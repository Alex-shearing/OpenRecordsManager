import { ManagedObjectKind, type ManagedObjectTypePropertyResponse } from '#lib/api/index.js';

/** Admin location-type forms only allow concrete kinds (user/group). */
export const LOCATION_TYPE_KINDS = [ManagedObjectKind.USER, ManagedObjectKind.GROUP] as const;

export type LocationTypeKind = (typeof LOCATION_TYPE_KINDS)[number];

export function asLocationTypeKind(value: string | undefined): LocationTypeKind | undefined {
	return LOCATION_TYPE_KINDS.find(kind => kind === value);
}

export function isBuiltinResourceId(id: string): boolean {
	return id.startsWith('builtin:');
}

export function sortById<T extends { id: string }>(items: T[]): T[] {
	return [...items].sort((a, b) => a.id.localeCompare(b.id));
}

/** Effective properties inherited from the selected base type (empty when none). */
export function inheritedPropertiesFromBase(
	types: ReadonlyArray<{ id: string; effectiveProperties?: ManagedObjectTypePropertyResponse[] }>,
	baseTypeId: string
): ManagedObjectTypePropertyResponse[] {
	if (!baseTypeId) {
		return [];
	}
	return types.find(type => type.id === baseTypeId)?.effectiveProperties ?? [];
}

/** Drop ids that are locked by inheritance so they are not re-declared on save. */
export function withoutInheritedPropertyIds(
	selectedIds: readonly string[],
	inherited: ReadonlyArray<ManagedObjectTypePropertyResponse>
): string[] {
	const locked = new Set(inherited.map(entry => entry.property.id));
	return selectedIds.filter(id => !locked.has(id));
}

export function parseContentTypes(value: string): string[] {
	return value
		.split(',')
		.map(entry => entry.trim())
		.filter(Boolean);
}

export function formatContentTypes(values: string[] | undefined): string {
	return values?.join(', ') ?? '';
}

export function buildTypePropertyAssignments(
	selectedIds: string[],
	defaults: Record<string, string>
): Array<{ property: string; default?: unknown }> {
	return selectedIds.map(property => {
		const raw = defaults[property]?.trim();
		if (!raw) {
			return { property };
		}
		try {
			return { property, default: JSON.parse(raw) };
		} catch {
			return { property, default: raw };
		}
	});
}

export function assignmentsFromTypeProperties(properties: Array<{ property: { id: string }; default?: unknown }>): {
	selectedIds: string[];
	defaults: Record<string, string>;
} {
	const selectedIds: string[] = [];
	const defaults: Record<string, string> = {};
	for (const entry of properties) {
		selectedIds.push(entry.property.id);
		if (entry.default !== undefined && entry.default !== null) {
			defaults[entry.property.id] = typeof entry.default === 'string' ? entry.default : JSON.stringify(entry.default);
		}
	}
	return { selectedIds, defaults };
}
