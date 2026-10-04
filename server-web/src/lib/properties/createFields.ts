import type {
	LocationTypePropertyResponse,
	ObjectPropertyResponse,
	RecordTypePropertyResponse,
} from '$lib/api/types.gen';
import { objectPropertyName } from '$lib/i18n/labels';

export type TypePropertyAssignment = {
	property: ObjectPropertyResponse;
	default?: unknown;
};

const AUTO_MANAGED = new Set(['builtin:date_created', 'builtin:date_modified']);
const LOCATION_IDENTITY = new Set(['builtin:username', 'builtin:name']);
const RECORD_IDENTITY = new Set(['builtin:title']);

export type CreateFieldFilter = {
	/** Omit username/name when those are top-level location create fields. */
	omitLocationIdentity?: boolean;
	/** Omit title when it is a top-level record create field. */
	omitRecordIdentity?: boolean;
};

export function toTypePropertyAssignments(
	properties: ReadonlyArray<RecordTypePropertyResponse | LocationTypePropertyResponse> | undefined
): TypePropertyAssignment[] {
	return [...(properties ?? [])].map(entry => ({
		property: entry.property,
		default: entry.default,
	}));
}

export function filterCreateFields(
	assignments: ReadonlyArray<TypePropertyAssignment>,
	options: CreateFieldFilter = {}
): TypePropertyAssignment[] {
	return assignments
		.filter(assignment => {
			const id = assignment.property.id;
			if (assignment.property.userHidden) {
				return false;
			}
			if (AUTO_MANAGED.has(id)) {
				return false;
			}
			if (options.omitLocationIdentity && LOCATION_IDENTITY.has(id)) {
				return false;
			}
			if (options.omitRecordIdentity && RECORD_IDENTITY.has(id)) {
				return false;
			}
			return true;
		})
		.toSorted((left, right) =>
			objectPropertyName(left.property.id).localeCompare(objectPropertyName(right.property.id))
		);
}

/** Seed create form values from type assignment defaults (type override, else property default). */
export function seedCreateValues(assignments: ReadonlyArray<TypePropertyAssignment>): Record<string, unknown> {
	const values: Record<string, unknown> = {};
	for (const assignment of assignments) {
		const fallback = assignment.default !== undefined ? assignment.default : assignment.property.defaultValue;
		if (fallback !== undefined && fallback !== null) {
			values[assignment.property.id] = structuredClone(fallback);
		}
	}
	return values;
}

/** Prefill edit form values from an existing properties map for the given fields. */
export function valuesFromExistingProperties(
	assignments: ReadonlyArray<TypePropertyAssignment>,
	properties: Record<string, unknown> | undefined
): Record<string, unknown> {
	const values: Record<string, unknown> = {};
	for (const assignment of assignments) {
		const id = assignment.property.id;
		if (properties && Object.prototype.hasOwnProperty.call(properties, id)) {
			const current = properties[id];
			if (current !== undefined && current !== null) {
				values[id] = structuredClone(current);
			}
		}
	}
	return values;
}

/** Drop empty/null values before submit so server defaults remain intact. */
export function compactPropertyValues(values: Record<string, unknown>): Record<string, unknown> {
	const result: Record<string, unknown> = {};
	for (const [key, value] of Object.entries(values)) {
		if (value === undefined || value === null || value === '') {
			continue;
		}
		if (Array.isArray(value) && value.every(item => item === '' || item === null || item === undefined)) {
			continue;
		}
		result[key] = value;
	}
	return result;
}
