import type { SimpleObjectPropertyResponse } from '#lib/api/types.gen.js';
import { preferredLocale } from '#lib/i18n/locale.js';
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

	if (definition?.type === 'date') {
		if (typeof value !== 'string' && typeof value !== 'number') {
			return empty;
		}
		return formatDateTime(value) ?? String(value);
	}
	if (definition?.type === 'list_item' && typeof value === 'string') {
		return listElementName(value);
	}
	if (definition?.type === 'list_multiple' && Array.isArray(value)) {
		return value
			.map(item =>
				typeof item === 'string' ? listElementName(item) : formatObjectPropertyValue(definition, item, empty)
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

const DATE_TIME_FORMAT: Intl.DateTimeFormatOptions = {
	day: '2-digit',
	month: '2-digit',
	year: 'numeric',
	hour: '2-digit',
	minute: '2-digit',
	second: '2-digit',
	hourCycle: 'h23',
};

const dateTimeFormatters = new Map<string, Intl.DateTimeFormat>();

function dateTimeFormatter(locale: string): Intl.DateTimeFormat {
	let formatter = dateTimeFormatters.get(locale);
	if (!formatter) {
		formatter = new Intl.DateTimeFormat(locale, DATE_TIME_FORMAT);
		dateTimeFormatters.set(locale, formatter);
	}
	return formatter;
}

/** Formats with {@link Intl.DateTimeFormat}, or `undefined` if not a valid date. */
export function formatDateTime(value: string | number): string | undefined {
	const date = new Date(value);
	if (Number.isNaN(date.getTime())) {
		return undefined;
	}
	return dateTimeFormatter(preferredLocale()).format(date);
}
