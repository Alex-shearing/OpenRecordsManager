import { WebController } from '$lib/api';
import { goto } from '$app/navigation';
import { page } from '$app/state';
import { client } from '$lib/api/client.gen';

export const ssr = false;
export const prerender = false;

client.interceptors.request.use(request => {
	const csrfToken = getCookie('XSRF-TOKEN');
	if (csrfToken) {
		request.headers.set('X-XSRF-TOKEN', csrfToken);
	}

	return request;
});

client.interceptors.response.use((response, request) => {
	if (
		response.status === 503 &&
		response.headers.get('X-ORM-Schema-Upgrade-Required') === 'true' &&
		['/maintenance', '/setup'].includes(page.url.pathname)
	) {
		const redirect = encodeURIComponent(page.url.pathname + page.url.search);
		throw goto(`/maintenance?redirect=${redirect}`);
	}

	const path = new URL(request.url).pathname;
	if (
		response.status === 401 &&
		!path.startsWith('/api/auth/') &&
		!path.startsWith('/api/database/') &&
		!path.startsWith('/api/web/')
	) {
		throw goto(`/login?redirect=${page.url.pathname}`);
	}

	if (response.status >= 400) {
		console.error(response.url + ' returned ' + response.status);
	}

	return response;
});

function getCookie(name: string): string | null {
	if (typeof document === 'undefined') return null;
	const value = `; ${document.cookie}`;
	const parts = value.split(`; ${name}=`);
	if (parts.length === 2) return parts.pop()?.split(';').shift() ?? null;
	return null;
}

export async function load() {
	const { data } = await WebController.branding();
	const brnd = data?.success
		? data.data
		: {
				productName: 'Open Records Manager',
				logoUrl: '',
				faviconUrl: '',
				primaryColor: '#1d4ed8',
				supportUrl: '',
			};

	return {
		branding: brnd,
	};
}
