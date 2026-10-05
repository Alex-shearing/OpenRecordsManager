import type { SimpleObjectPropertyResponse } from '#lib/api/types.gen.js';
import { listElementName } from '#lib/i18n/labels.js';

/**
 * Formats a stored object-property value for display.
 * List item ids are resolved to their translated display names.
 */
export function formatObjectPropertyValue(
	definition: Pick<SimpleObjectPropertyResponse, 'type'> | undefined,
	value: unknown,
	empty = '',
): string {
	if (value == null) {
		return empty;
	}

	if (definition?.type === 'list_item' && typeof value === 'string') {
		return listElementName(value);
	}
	if (definition?.type === 'list_multiple' && Array.isArray(value)) {
		return value
			.map(item =>
				typeof item === 'string' ? listElementName(item) : formatObjectPropertyValue(definition, item, empty),
			)
			.join(', ');
	}

	if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
		return String(value);
	}
	if (Array.isArray(value)) {
		return value.map(item => formatObjectPropertyValue(definition, item, empty)).join(', ');
	}
	try {
		return JSON.stringify(value);
	} catch {
		return empty;
	}
}
