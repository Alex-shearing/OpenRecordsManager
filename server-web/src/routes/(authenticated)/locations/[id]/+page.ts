import { LocationController } from '#lib/api/index.js';
import type { ActionResponse } from '#lib/api/types.gen.js';
import { getApiClient } from '#lib/api-client.js';

export async function load({ params, parent }) {
	const [layout, locationResult] = await Promise.all([
		parent(),
		LocationController.getLocation({
			client: getApiClient(),
			path: { id: params.id },
		}),
	]);

	if (locationResult.error) {
		return {
			location: undefined,
			actions: [] as ActionResponse[],
			loadError: locationResult.error ?? layout.error,
		};
	}

	const actionsResult = await LocationController.listLocationActions({
		client: getApiClient(),
		path: { id: params.id },
	});

	return {
		location: locationResult.data.data,
		actions: actionsResult.data?.data ?? [],
		loadError: actionsResult.error ?? layout.error,
	};
}
