import { LocationKind } from '#lib/api/index.js';

/** Location types only allow user/group; {@link LocationKind.ANY} is for relationship constraints. */
export const LOCATION_TYPE_KINDS = [LocationKind.USER, LocationKind.GROUP] as const;

export const PROPERTY_TYPES = [
	'string',
	'number',
	'decimal',
	'boolean',
	'uuid',
	'object',
	'date',
	'string_list',
	'int_list',
	'list_item',
	'list_multiple',
] as const;
export type PropertyTypeName = (typeof PROPERTY_TYPES)[number];

export function isBuiltinResourceId(id: string): boolean {
	return id.startsWith('builtin:');
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
