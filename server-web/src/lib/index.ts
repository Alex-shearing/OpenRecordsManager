import createClient from 'openapi-fetch';
import type { paths } from './types/schema';
import { page } from '$app/state';
import { goto } from '$app/navigation';
import { config } from '$lib/config.svelte';
import { getCookie } from '$lib/api';

let cachedClient: ReturnType<typeof createClient<paths>> | undefined;

export function getClient() {
	if (cachedClient) return cachedClient;

	cachedClient = createClient<paths>({
		baseUrl: config.apiUrl(),
		headers: {
			'X-Client-Platform': 'Web-Client'
		},
		credentials: 'include'
	});

	cachedClient.use({
		onRequest({ request }) {
			// Extract the token from browser cookies and attach it for CSFR protection
			const csrfToken = getCookie('XSRF-TOKEN');
			if (csrfToken) {
				request.headers.set('X-XSRF-TOKEN', csrfToken);
			}

			return request;
		},
		onResponse({ response, schemaPath }) {
			// Handle the schema update required response
			if (
				response.status === 503 &&
				response.headers.get('X-ORM-Schema-Upgrade-Required') === 'true' &&
				!["/maintenance", "/setup"].includes(page.url.pathname)
			) {
				const redirect = encodeURIComponent(page.url.pathname + page.url.search);
				throw goto(`/maintenance?redirect=${redirect}`);
			}

			if (
				response.status === 401 &&
				schemaPath !== '/api/auth/login/{provider}' &&
				!schemaPath.startsWith('/api/database/')
			) {
				throw goto(`/login?redirect=${page.url.pathname}`);
			}
			if (response.status >= 400) {
				console.error(response.url + ' returned ' + response.status);
			}
		}
	});

	return cachedClient;
}
