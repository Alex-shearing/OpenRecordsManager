import { env } from '$env/dynamic/public';
import { goto } from '$app/navigation';
import { page } from '$app/state';
import { createClient, createConfig, type Client } from '$lib/api/client';

const SCHEMA_UPGRADE_HEADER = 'X-ORM-Schema-Upgrade-Required';

const baseUrl = (
	env.PUBLIC_API_URL || (typeof window !== 'undefined' ? (window.__ORM_UI__?.apiBaseUrl ?? '') : '')
).replace(/\/$/, '');

let apiClient: Client | undefined;

/**
 * Creates the app API client with SvelteKit's fetch and registers interceptors.
 * Call once from the root layout load; use {@link getApiClient} everywhere else.
 */
export function createApiClient(fetch: typeof globalThis.fetch): Client {
	const client = createClient(
		createConfig({
			baseUrl,
			fetch,
			credentials: 'include',
			headers: {
				'X-Client-Platform': 'Web-Client',
			},
		})
	);

	client.interceptors.request.use(request => {
		const csrfToken = getCookie('XSRF-TOKEN');
		if (csrfToken) {
			request.headers.set('X-XSRF-TOKEN', csrfToken);
		}

		return request;
	});

	client.interceptors.response.use((response, request) => {
		handleSchemaUpdateRequired(response, request);

		if (response.status >= 400) {
			console.error(response.url + ' returned ' + response.status);
		}

		return response;
	});

	apiClient = client;
	return client;
}

export function getApiClient(): Client {
	if (!apiClient) {
		throw new Error('API client has not been created yet. Ensure the root layout has loaded.');
	}

	return apiClient;
}

function handleSchemaUpdateRequired(response: Response, request: Request): Response {
	if (page.route.id?.startsWith('/(guest)/(maintenance)')) {
		return response;
	}

	if (request.url.startsWith('/api/database/') || request.url.startsWith('/api/web')) {
		return response;
	}

	if (response.status === 503 && response.headers.get(SCHEMA_UPGRADE_HEADER) === 'true') {
		const dest = new URL('/maintenance', document.baseURI);
		dest.searchParams.append('redirect', encodeURIComponent(page.url.pathname + page.url.search));
		throw goto(dest);
	}

	return response;
}

function getCookie(name: string): string | null {
	if (typeof document === 'undefined') return null;
	const value = `; ${document.cookie}`;
	const parts = value.split(`; ${name}=`);
	if (parts.length === 2) return parts.pop()?.split(';').shift() ?? null;
	return null;
}
