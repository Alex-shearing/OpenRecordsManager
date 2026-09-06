/// <reference no-default-lib="true"/>
/// <reference lib="esnext" />
/// <reference lib="webworker" />
/// <reference types="@sveltejs/kit" />

import { base, build, files, prerendered, version } from '$service-worker';

const sw = self as unknown as ServiceWorkerGlobalScope;

const CACHE = `cache-${version}`;

/** SPA shell + runtime env are not always listed in build/files/prerendered. */
const SHELL = `${base}/index.html`;
const RUNTIME_ENV = `${base}/_app/env.js`;

const ASSETS = Array.from(
	new Set([...build, ...files, ...prerendered, SHELL, RUNTIME_ENV, `${base}/`])
);

sw.addEventListener('install', event => {
	async function addFilesToCache() {
		const cache = await caches.open(CACHE);
		await Promise.all(
			ASSETS.map(async asset => {
				try {
					await cache.add(asset);
				} catch {
					// Skip missing/unreachable assets so install still completes.
				}
			})
		);
	}

	event.waitUntil(addFilesToCache());
});

sw.addEventListener('activate', event => {
	async function deleteOldCaches() {
		for (const key of await caches.keys()) {
			if (key !== CACHE) {
				await caches.delete(key);
			}
		}
	}

	event.waitUntil(deleteOldCaches());
});

sw.addEventListener('fetch', event => {
	if (event.request.method !== 'GET') {
		return;
	}

	const url = new URL(event.request.url);

	// Never cache API traffic (auth cookies, CSRF, live data).
	if (url.pathname.startsWith(`${base}/api`) || url.pathname.startsWith('/api')) {
		return;
	}

	async function respond(): Promise<Response> {
		const cache = await caches.open(CACHE);

		if (ASSETS.includes(url.pathname)) {
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
				const shell =
					(await cache.match(SHELL)) ??
					(await cache.match(`${base}/`)) ??
					(await cache.match(event.request));
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
