import { WebController } from '$lib/api';
import { createApiClient } from '$lib/api-client';
import faviconAsset from '$lib/assets/favicon.ico';

export const ssr = false;
export const prerender = false;

const DEFAULT_BRANDING = {
	productName: 'Open Records Manager',
	logoUrl: '',
	faviconUrl: faviconAsset,
	primaryColor: '#1d4ed8',
	supportUrl: '',
};

export async function load({ fetch }) {
	const client = createApiClient(fetch);
	try {
		const { data } = await WebController.branding({ client });
		return {
			branding: data?.data || DEFAULT_BRANDING,
			online: true,
		};
	} catch {
		// Offline / API unreachable — still boot the SPA shell from cache.
		return {
			branding: DEFAULT_BRANDING,
			online: false,
		};
	}
}
