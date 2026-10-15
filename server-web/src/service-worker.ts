/// <reference no-default-lib="true"/>
/// <reference lib="esnext" />
/// <reference lib="webworker" />
/// <reference types="@sveltejs/kit" />

import { version } from '$app/env';
import { assets, immutable, prerendered } from '$app/manifest';
import { asset, resolve } from '$app/paths';

const sw = self as unknown as ServiceWorkerGlobalScope;
const CACHE_NAME = `cache-${version}`;

const I18N_CACHE = 'orm-i18n-v1';
const TRANSLATIONS_CATALOG_PATH = '/api/translations/catalog';

const SHELL = asset('index.html' as any);

/** Manifest assets, SPA shell, runtime env */
const PRECACHE = [
	...new Set([
		...assets.map(a => resolve(a.path)),
		...immutable.map(a => resolve(a.path)),
		...prerendered.map(a => resolve(a.path)),
		resolve('_app/env.js'),
		SHELL,
		'/',
	]),
];

async function precache(cache: Cache, urls: string[]): Promise<void> {
	await Promise.allSettled(urls.map(url => cache.add(url)));

	// Navigate requests use `/`; ensure that key resolves even if only index.html was stored.
	const shell = (await cache.match(SHELL)) ?? (await cache.match('/'));
	if (shell) {
		if (!(await cache.match(SHELL))) {
			await cache.put(SHELL, shell.clone());
		}
		if (!(await cache.match('/'))) {
			await cache.put('/', shell.clone());
		}
	}
}

/** Stable cache key: URL only + locale (ignore cookies / CSRF / Vary noise). */
function translationCatalogCacheKey(request: Request): Request {
	const locale = request.headers.get('Accept-Language')?.split(',')[0]?.trim() || 'en';
	const url = new URL(TRANSLATIONS_CATALOG_PATH, sw.location.origin);
	url.searchParams.set('__locale', locale);
	return new Request(url.href);
}

async function matchAnyCachedCatalog(cache: Cache): Promise<Response | undefined> {
	for (const request of await cache.keys()) {
		const cached = await cache.match(request);
		if (cached) {
			return cached;
		}
	}
}

async function respondTranslationCatalog(request: Request): Promise<Response> {
	const cache = await caches.open(I18N_CACHE);
	const key = translationCatalogCacheKey(request);

	try {
		const response = await fetch(request);
		if (response.ok) {
			void cache.put(key, response.clone());
		}
		return response;
	} catch {
		const cached = (await cache.match(key)) ?? (await matchAnyCachedCatalog(cache));
		if (cached) {
			return cached;
		}
		return Response.error();
	}
}

sw.addEventListener('install', event => {
	event.waitUntil(caches.open(CACHE_NAME).then(cache => precache(cache, PRECACHE)));
});

sw.addEventListener('activate', event => {
	event.waitUntil(
		caches.keys().then(keys => {
			return Promise.all(
				keys.map(key => {
					// Keep the i18n runtime cache across deploys; only drop old asset caches.
					if (key !== CACHE_NAME && key !== I18N_CACHE) {
						return caches.delete(key);
					}
				})
			);
		})
	);
});

sw.addEventListener('fetch', event => {
	if (event.request.method !== 'GET') {
		return;
	}

	const url = new URL(event.request.url);

	// Translation catalog: network-first, locale-keyed cache for offline.
	if (url.pathname === TRANSLATIONS_CATALOG_PATH) {
		event.respondWith(respondTranslationCatalog(event.request));
		return;
	}

	// Never cache other API traffic (auth cookies, CSRF, live data).
	if (url.pathname.startsWith('/api')) {
		return;
	}

	const isNavigate = event.request.mode === 'navigate';
	const isPrecached = PRECACHE.includes(url.pathname);

	// Only intercept navigations and precached production assets.
	if (!isNavigate && !isPrecached) {
		return;
	}

	async function respond(): Promise<Response> {
		const cache = await caches.open(CACHE_NAME);

		if (isPrecached) {
			const cached = await cache.match(url.pathname);
			if (cached) {
				return cached;
			}
		}

		try {
			const response = await fetch(event.request);

			if (!(response instanceof Response)) {
				throw new Error('invalid response from fetch');
			}

			// Fill gaps if install-time precache missed a file (e.g. env.js race).
			if (isPrecached && response.ok) {
				void cache.put(url.pathname, response.clone());
			}

			return response;
		} catch {
			if (isNavigate) {
				const shell = (await cache.match(SHELL)) ?? (await cache.match('/')) ?? (await cache.match(event.request));
				if (shell) {
					return shell;
				}
			}

			const cached = await cache.match(event.request);
			if (cached) {
				return cached;
			}

			// Never reject respondWith — that surfaces as a hard page failure.
			return Response.error();
		}
	}

	event.respondWith(respond());
});
