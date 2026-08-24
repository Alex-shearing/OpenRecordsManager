export type BrandingConfig = {
	productName: string;
	logoUrl: string;
	faviconUrl: string;
	primaryColor: string;
	supportUrl: string;
};

export type RuntimeConfig = {
	apiBaseUrl: string;
	branding: BrandingConfig;
};

export const defaultRuntimeConfig: RuntimeConfig = {
	apiBaseUrl: '',
	branding: {
		productName: 'Open Records Manager',
		logoUrl: '',
		faviconUrl: '',
		primaryColor: '#1d4ed8',
		supportUrl: ''
	}
};

let cached: RuntimeConfig | null = null;
let loadPromise: Promise<RuntimeConfig> | null = null;

export function mergeConfig(partial: Partial<RuntimeConfig> | null | undefined): RuntimeConfig {
	return {
		apiBaseUrl: partial?.apiBaseUrl?.replace(/\/$/, '') ?? defaultRuntimeConfig.apiBaseUrl,
		branding: {
			...defaultRuntimeConfig.branding,
			...(partial?.branding ?? {})
		}
	};
}

/** Absolute API URL for paths (e.g. OAuth redirects). Requires apiBaseUrl. */
export function apiUrl(path: string, config: RuntimeConfig = getRuntimeConfigSync()): string {
	if (!config.apiBaseUrl) {
		throw new Error('apiBaseUrl is required (set UI_API_BASE_URL)');
	}
	const normalized = path.startsWith('/') ? path : `/${path}`;
	return `${config.apiBaseUrl}${normalized}`;
}

export function getRuntimeConfigSync(): RuntimeConfig {
	return cached ?? defaultRuntimeConfig;
}

export async function loadRuntimeConfig(): Promise<RuntimeConfig> {
	if (cached) return cached;
	if (loadPromise) return loadPromise;

	loadPromise = (async () => {
		try {
			const response = await fetch('/config.json', { cache: 'no-store' });
			if (!response.ok) {
				cached = defaultRuntimeConfig;
				return cached;
			}
			cached = mergeConfig(await response.json());
			return cached;
		} catch {
			cached = defaultRuntimeConfig;
			return cached;
		}
	})();

	return loadPromise;
}
