import { getClient } from "$lib";
import type { components } from "./types/schema";

type UiHostConfig = {
	/** Empty string = same origin (relative API calls). */
	apiBaseUrl?: string;
};

type BrandingConfig = components['schemas']['WebBrandingResponse'];

const defaultBranding = {
    productName: 'Open Records Manager',
    logoUrl: '',
    faviconUrl: '',
    primaryColor: '#1d4ed8',
    supportUrl: ''
};

class WebConfig {
    #apiUrl = $state<string>();
    #brandingData = $state<BrandingConfig>();

    async loadApiBase() {
        if (this.#apiUrl !== undefined) return this.#apiUrl;

        try {
            const response = await fetch('/config.json', { cache: 'no-store' });
            if (!response.ok) return '';
            const body = (await response.json()) as UiHostConfig;
            this.#apiUrl = body.apiBaseUrl?.replace(/\/$/, '') ?? '';
        } catch {
            this.#apiUrl = ''
        }

        return this.#apiUrl;
    }

    apiUrl(path?: string) {
        const normalized = path?.startsWith('/') ? path : `/${path || ''}`;
        const apiBase = this.#apiUrl ?? '';

        if (!apiBase) {
            return normalized;
        }
        return `${apiBase}${normalized}`;
    }

    getConfig() {
        if (this.#brandingData !== undefined) return this.#brandingData;

        return defaultBranding;
    }

    async loadConfig() {
        if (this.#brandingData !== undefined) return this.#brandingData;

        await this.loadApiBase();

        const { data } = await getClient().GET('/api/web');
        
        if (data) {
            return data.data;
        }

        return {
            productName: 'Open Records Manager',
            logoUrl: '',
            faviconUrl: '',
            primaryColor: '#1d4ed8',
            supportUrl: ''
        };
    }
}

// Export a single instance to share globally
export const config = new WebConfig();