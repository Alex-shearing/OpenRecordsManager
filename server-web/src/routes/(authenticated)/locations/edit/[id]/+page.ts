import { error } from '@sveltejs/kit';
import { LocationController, UserController } from '$lib/api';
import { getApiClient } from '$lib/api-client';

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
			user: undefined,
			loadError: locationResult.error ?? layout.error,
		};
	}

	const location = locationResult.data.data;
	let user = undefined;
	let loadError = layout.error;

	if (location.kind === 'user') {
		const userResult = await UserController.get({
			client: getApiClient(),
			path: { id: params.id },
		});

		loadError = userResult.error;
		user = userResult.data?.data;
	}

	return {
		location,
		user,
		loadError,
	};
}
