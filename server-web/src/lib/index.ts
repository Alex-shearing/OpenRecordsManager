import createClient from 'openapi-fetch';
import type { paths } from './types/schema';
import { page } from '$app/state';
import { goto } from '$app/navigation';

// Helper function to read cookies in the browser environment
function getCookie(name: string): string | null {
	if (typeof document === 'undefined') return null;
	const value = `; ${document.cookie}`;
	const parts = value.split(`; ${name}=`);
	if (parts.length === 2) return parts.pop()?.split(';').shift() ?? null;
	return null;
}

export const client = createClient<paths>({
	headers: {
		'X-Client-Platform': 'Web-Client'
	},
	credentials: 'include'
});

client.use({
	onRequest({ request }) {
		// Extract the token from browser cookies and attach it for CSFR protection
		const csrfToken = getCookie('XSRF-TOKEN');
		if (csrfToken) {
			request.headers.set('X-XSRF-TOKEN', csrfToken);
		}

		return request;
	},
	onResponse({ response, schemaPath }) {
		if (response.status === 401 && schemaPath !== '/api/auth/login/{provider}') {
			throw goto(`/login?redirect=${page.url.pathname}`);
		}
		if (response.status >= 400) {
			console.error(response.statusText);
		}
	}
});
