import { browser } from '$app/environment';

const STORAGE_KEY = 'orm.locale';

export function preferredLocale(): string {
	if (browser) {
		const stored = localStorage.getItem(STORAGE_KEY);
		if (stored) {
			return stored;
		}
		const nav = navigator.language;
		if (nav) {
			return nav;
		}
	}
	return 'en';
}

export function setPreferredLocale(locale: string): void {
	if (browser) {
		localStorage.setItem(STORAGE_KEY, locale);
	}
}
