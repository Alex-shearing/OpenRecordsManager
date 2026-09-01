import { WebController } from '$lib/api';
import { client } from '$lib/api/client.gen';
import { handleSchemaUpdateRequired } from '$lib/api-client.config';

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
	handleSchemaUpdateRequired(response, request);

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
