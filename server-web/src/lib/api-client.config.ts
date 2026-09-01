import { env } from '$env/dynamic/public';
import type { CreateClientConfig } from './api/client.gen';

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
