import { WebController } from '$lib/api';
import { createApiClient } from '$lib/api-client';

export const ssr = false;
export const prerender = false;

export async function load({ fetch }) {
	const client = createApiClient(fetch);
	const { data } = await WebController.branding({ client });
	const branding = data?.data || {
		productName: 'Open Records Manager',
		logoUrl: '',
		faviconUrl: '',
		primaryColor: '#1d4ed8',
		supportUrl: '',
	};

	return {
		branding,
	};
}
