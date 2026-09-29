import { TranslationController, WebController } from '$lib/api';
import { createApiClient } from '$lib/api-client';
import faviconAsset from '$lib/assets/favicon.ico';
import { setCatalog } from '$lib/i18n/catalog';

export const ssr = false;
export const prerender = false;

const DEFAULT_BRANDING = {
	logoUrl: '',
	faviconUrl: faviconAsset,
	primaryColor: '#1d4ed8',
	supportUrl: '',
};

export async function load({ fetch }) {
	const client = createApiClient(fetch);
	const [branding, catalogResult] = await Promise.all([
		WebController.branding({ client }),
		TranslationController.getTranslationCatalog({ client }),
	]);

	const catalog = catalogResult.data?.data;
	if (catalog) {
		setCatalog({
			locale: catalog.locale ?? 'en',
			messages: catalog.messages ?? {},
		});
	}

	return {
		branding: branding.data?.data || DEFAULT_BRANDING,
		online: !!branding.response,
		catalogLocale: catalog?.locale ?? 'en',
	};
}
