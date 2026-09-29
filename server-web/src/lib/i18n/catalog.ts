import type { ApiFieldError } from '$lib/api';
import { writable, get } from 'svelte/store';

export type TranslationCatalog = {
	locale: string;
	messages: Record<string, string>;
};

const catalogStore = writable<TranslationCatalog>({
	locale: 'en',
	messages: {},
});

export function setCatalog(next: TranslationCatalog): void {
	catalogStore.set({
		locale: next.locale || 'en',
		messages: next.messages ?? {},
	});
}

function formatMessage(template: string, args: Array<string | number>): string {
	let message = template;
	for (let i = 0; i < args.length; i++) {
		message = message.replaceAll(`{${i}}`, String(args[i]));
	}
	return message;
}

/** Resolve a message key; returns the key itself when missing. Supports `{0}`-style args. */
export function t(key: string | null | undefined, ...args: Array<string | number>): string {
	if (!key) {
		return '';
	}
	const messages = get(catalogStore).messages;
	const template = messages[key] ?? key;
	return args.length > 0 ? formatMessage(template, args) : template;
}

/** Resolve a message key; returns `fallback` when the key is missing. Supports `{0}`-style args. */
export function tx(key: string | null | undefined, fallback: string, ...args: Array<string | number>): string {
	if (!key) {
		return fallback;
	}
	const messages = get(catalogStore).messages;
	const template = messages[key];
	if (template == null) {
		return args.length > 0 ? formatMessage(fallback, args) : fallback;
	}
	return args.length > 0 ? formatMessage(template, args) : template;
}

/**
 * Resolve a failed API error for display.
 * `error.error` is a bare wire code (`authentication_failed`) or a full catalog key (`web.*`).
 */
export function tApiErrorResponse(error: ApiFieldError): string {
	const key = error.error.startsWith('web.') ? error.error : `error.${error.error}`;
	return t(key, ...(error.errorArgs ?? []));
}
