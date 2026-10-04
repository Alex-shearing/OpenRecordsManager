import type { RouteId, RouteParams } from '$app/types';

/** Route IDs that `resolve(route)` accepts without params. */
export type StaticRouteId = {
	[K in RouteId]: RouteParams<K> extends Record<string, never> ? K : never;
}[RouteId];
