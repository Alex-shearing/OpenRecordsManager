import { env } from '$env/dynamic/public';
import { goto } from '$app/navigation';
import { page } from '$app/state';
import { createClient, createConfig, type Client } from '$lib/api/client';

const SCHEMA_UPGRADE_HEADER = 'X-ORM-Schema-Upgrade-Required';
export const CSRF_HEADER = 'X-CSRF-TOKEN';
export const AUDIT_COMMENT_HEADER = 'X-ORM-Audit-Comment';

const baseUrl = (
	env.PUBLIC_API_URL || (typeof window !== 'undefined' ? (window.__ORM_UI__?.apiBaseUrl ?? '') : '')
).replace(/\/$/, '');

let apiClient: Client | undefined;
/** Set from API response headers; required when UI and API are on different hostnames. */
let csrfTokenFromHeader: string | null = null;
let refreshInFlight: Promise<boolean> | null = null;

/**
 * Creates the app API client with SvelteKit's fetch and registers interceptors.
 * Call once from the root layout load; use {@link getApiClient} everywhere else.
 */
export function createApiClient(fetchImpl: typeof globalThis.fetch): Client {
	const client = createClient(
		createConfig({
			baseUrl,
			fetch: fetchImpl,
			credentials: 'include',
			headers: {
				'X-Client-Platform': 'Web-Client',
			},
		})
	);

	client.interceptors.request.use(request => {
		if (csrfTokenFromHeader) {
			request.headers.set(CSRF_HEADER, csrfTokenFromHeader);
		}

		return request;
	});

	client.interceptors.response.use(async (response, request) => {
		const headerToken = response.headers.get(CSRF_HEADER);
		if (headerToken) {
			csrfTokenFromHeader = headerToken;
		}

		handleSchemaUpdateRequired(response, request);

		if (response.status === 401 && shouldAttemptRefresh(request.url)) {
			const refreshed = await refreshSession(fetchImpl);
			if (refreshed) {
				const retryRequest = new Request(request, {
					headers: new Headers(request.headers),
				});
				if (csrfTokenFromHeader) {
					retryRequest.headers.set(CSRF_HEADER, csrfTokenFromHeader);
				}
				const retryResponse = await fetchImpl(retryRequest);
				const retryCsrf = retryResponse.headers.get(CSRF_HEADER);
				if (retryCsrf) {
					csrfTokenFromHeader = retryCsrf;
				}
				return retryResponse;
			}

			await redirectToLogin();
		}

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

/** Optional request headers carrying an audit comment when non-blank. */
export function auditHeaders(comment: string): { [AUDIT_COMMENT_HEADER]: string } | undefined {
	const trimmed = comment.trim();
	return trimmed ? { [AUDIT_COMMENT_HEADER]: trimmed } : undefined;
}

function shouldAttemptRefresh(url: string): boolean {
	try {
		const path = new URL(url, 'http://localhost').pathname;
		return (
			!path.startsWith('/api/auth/login') &&
			!path.startsWith('/api/auth/refresh') &&
			!path.startsWith('/api/auth/logout') &&
			!path.startsWith('/api/auth/providers') &&
			!path.startsWith('/api/auth/callback') &&
			!path.startsWith('/api/auth/redirect')
		);
	} catch {
		return false;
	}
}

async function refreshSession(fetchImpl: typeof globalThis.fetch): Promise<boolean> {
	if (!refreshInFlight) {
		refreshInFlight = (async () => {
			try {
				const headers = new Headers({
					'X-Client-Platform': 'Web-Client',
				});
				if (csrfTokenFromHeader) {
					headers.set(CSRF_HEADER, csrfTokenFromHeader);
				}

				const response = await fetchImpl(`${baseUrl}/api/auth/refresh`, {
					method: 'POST',
					credentials: 'include',
					headers,
				});

				const headerToken = response.headers.get(CSRF_HEADER);
				if (headerToken) {
					csrfTokenFromHeader = headerToken;
				}

				return response.ok;
			} catch {
				return false;
			} finally {
				refreshInFlight = null;
			}
		})();
	}

	return refreshInFlight;
}

async function redirectToLogin(): Promise<void> {
	if (page.route.id?.startsWith('/(guest)')) {
		return;
	}

	const dest = new URL('/login', document.baseURI);
	dest.searchParams.append('redirect', page.url.pathname.substring(1) + page.url.search);
	throw goto(dest);
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
