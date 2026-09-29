import { UserController } from '$lib/api';
import { getApiClient } from '$lib/api-client';

export async function load({ parent }) {
	const data = await parent();

	const { data: actionsData, error: actionsError } = await UserController.listActions({
		client: getApiClient(),
		path: { id: data.me.id },
	});

	return {
		error: actionsError,
		actions: actionsData?.success ? actionsData.data : [],
	};
}
