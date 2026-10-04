import { LocationController, UserController } from '$lib/api';
import type { ActionResponse } from '$lib/api/types.gen';
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
			actions: [] as ActionResponse[],
			loadError: locationResult.error ?? layout.error,
		};
	}

	const location = locationResult.data.data;
	let user = undefined;
	let actions: ActionResponse[] = [];
	let loadError = layout.error;

	if (location.kind === 'user') {
		const [userResult, actionsResult] = await Promise.all([
			UserController.get({
				client: getApiClient(),
				path: { id: params.id },
			}),
			UserController.listActions({
				client: getApiClient(),
				path: { id: params.id },
			}),
		]);

		loadError = userResult.error ?? layout.error;
		user = userResult.data?.data;
		actions = actionsResult.data?.success ? actionsResult.data.data : [];
	}

	return {
		location,
		user,
		actions,
		loadError,
	};
}
