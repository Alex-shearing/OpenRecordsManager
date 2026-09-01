import { env } from '$env/dynamic/public';
import type { CreateClientConfig } from './api/client.gen';
import { goto } from '$app/navigation';
import { page } from '$app/state';
import type { Page } from '@sveltejs/kit';

const SCHEMA_UPGRADE_HEADER = 'X-ORM-Schema-Upgrade-Required';

export const baseUrl = (
	env.PUBLIC_API_URL || (typeof window !== 'undefined' ? (window.__ORM_UI__?.apiBaseUrl ?? '') : '')
).replace(/\/$/, '');

export const createClientConfig: CreateClientConfig = config => ({
	...config,
	baseUrl,
	credentials: 'include',
	headers: {
		'X-Client-Platform': 'Web-Client',
	},
});

export function handleSchemaUpdateRequired(response: Response, request: Request): Response {
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
