import { LocationController } from '#lib/api/index.js';
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
			loadError: locationResult.error ?? layout.error,
		};
	}

	return {
		location: locationResult.data.data,
		loadError: layout.error,
	};
}
