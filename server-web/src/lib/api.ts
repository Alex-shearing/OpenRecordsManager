import { config } from '$lib/config.svelte';

export const AUTH_COOKIE_NAME = 'ORM-Authentication';

/** Encode path segments that may contain `source:item` style IDs. */
export function encodePathId(id: string): string {
	return encodeURIComponent(id);
}

export function auditHeaders(comment?: string): Record<string, string> {
	if (!comment) return {};
	return { 'X-ORM-Audit-Comment': comment };
}

/** Best-effort clear of the auth cookie (HttpOnly cookies cannot be cleared from JS). */
export function clearAuthCookie(): void {
	if (typeof document === 'undefined') return;
	document.cookie = `${AUTH_COOKIE_NAME}=; Max-Age=0; Path=/; SameSite=Lax`;
	document.cookie = `${AUTH_COOKIE_NAME}=; Max-Age=0; Path=/; SameSite=None; Secure`;
}

export function getCookie(name: string): string | null {
	if (typeof document === 'undefined') return null;
	const value = `; ${document.cookie}`;
	const parts = value.split(`; ${name}=`);
	if (parts.length === 2) return parts.pop()?.split(';').shift() ?? null;
	return null;
}

/** Download a binary API response using cookie credentials. */
export async function downloadBinary(path: string, fallbackFilename: string): Promise<void> {
	const response = await fetch(config.apiUrl(path), {
		credentials: 'include',
		headers: {
			'X-Client-Platform': 'Web-Client',
			...(getCookie('XSRF-TOKEN') ? { 'X-XSRF-TOKEN': getCookie('XSRF-TOKEN')! } : {})
		}
	});
	if (!response.ok) {
		throw new Error(`Download failed (${response.status})`);
	}
	const disposition = response.headers.get('Content-Disposition');
	let filename = fallbackFilename;
	if (disposition) {
		const match = /filename\*?=(?:UTF-8'')?["']?([^"';]+)/i.exec(disposition);
		if (match?.[1]) filename = decodeURIComponent(match[1]);
	}
	const blob = await response.blob();
	const url = URL.createObjectURL(blob);
	const a = document.createElement('a');
	a.href = url;
	a.download = filename;
	a.click();
	URL.revokeObjectURL(url);
}

/** Upload a record revision via multipart. */
export async function uploadRevision(
	recordId: string,
	version: string,
	file: File
): Promise<{ ok: boolean; error?: string }> {
	const form = new FormData();
	form.append('stream', file);
	const csrf = getCookie('XSRF-TOKEN');
	const response = await fetch(
		config.apiUrl(`/api/records/${encodePathId(recordId)}/${encodePathId(version)}`),
		{
			method: 'PUT',
			credentials: 'include',
			headers: {
				'X-Client-Platform': 'Web-Client',
				...(csrf ? { 'X-XSRF-TOKEN': csrf } : {})
			},
			body: form
		}
	);
	if (!response.ok) {
		try {
			const body = await response.json();
			return { ok: false, error: body.error ?? `Upload failed (${response.status})` };
		} catch {
			return { ok: false, error: `Upload failed (${response.status})` };
		}
	}
	return { ok: true };
}

export function formatValue(value: unknown): string {
	if (value === null || value === undefined) return '—';
	if (typeof value === 'object') return JSON.stringify(value);
	return String(value);
}

export function parseOptionalJson(text: string): unknown {
	const trimmed = text.trim();
	if (!trimmed) return undefined;
	try {
		return JSON.parse(trimmed);
	} catch {
		return trimmed;
	}
}
