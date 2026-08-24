import { env } from '$env/dynamic/private';
import { json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import { defaultRuntimeConfig, mergeConfig } from '$lib/runtime-config';

export const GET: RequestHandler = async () => {
	const apiBaseUrl = env.UI_API_BASE_URL?.replace(/\/$/, '');
	if (!apiBaseUrl) {
		return json(
			{ error: 'UI_API_BASE_URL is required' },
			{ status: 500, headers: { 'Cache-Control': 'no-store' } }
		);
	}

	const config = mergeConfig({
		apiBaseUrl,
		branding: {
			productName: env.UI_PRODUCT_NAME ?? defaultRuntimeConfig.branding.productName,
			logoUrl: env.UI_LOGO_URL ?? defaultRuntimeConfig.branding.logoUrl,
			faviconUrl: env.UI_FAVICON_URL ?? defaultRuntimeConfig.branding.faviconUrl,
			primaryColor: env.UI_PRIMARY_COLOR ?? defaultRuntimeConfig.branding.primaryColor,
			supportUrl: env.UI_SUPPORT_URL ?? defaultRuntimeConfig.branding.supportUrl
		}
	});

	return json(config, {
		headers: {
			'Cache-Control': 'no-store'
		}
	});
};
