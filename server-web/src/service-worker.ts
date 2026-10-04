/// <reference no-default-lib="true"/>
/// <reference lib="esnext" />
/// <reference lib="webworker" />
/// <reference types="@sveltejs/kit" />

import { version } from '$app/env';
import { assets, immutable, prerendered } from '$app/manifest';
import { asset } from '$app/paths';

const sw = self as unknown as ServiceWorkerGlobalScope;
const CACHE_NAME = `cache-${version}`;

const SHELL = asset('index.html' as any);

const ASSETS_TO_CACHE = [
	...assets.map(a => a.path), // Static files from /static folder
	...immutable.map(a => a.path), // Immutable JS/CSS chunks compiled by Vite
	...prerendered.map(a => a.path), // Prerendered HTML pages/endpoints
];

sw.addEventListener('install', event => {
	event.waitUntil(
		caches.open(CACHE_NAME).then(cache => {
			return cache.addAll(ASSETS_TO_CACHE);
		})
	);
});

sw.addEventListener('activate', event => {
	event.waitUntil(
		caches.keys().then(keys => {
			return Promise.all(
				keys.map(key => {
					if (key !== CACHE_NAME) {
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

	// Never cache API traffic (auth cookies, CSRF, live data).
	if (url.pathname.startsWith('/api')) {
		return;
	}

	async function respond(): Promise<Response> {
		const cache = await caches.open(CACHE_NAME);

		if (ASSETS_TO_CACHE.includes(url.pathname)) {
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

			return response;
		} catch {
			if (event.request.mode === 'navigate') {
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
			return new Response('Offline', {
				status: 503,
				statusText: 'Service Unavailable',
				headers: { 'Content-Type': 'text/plain; charset=utf-8' },
			});
		}
	}

	event.respondWith(respond());
});
