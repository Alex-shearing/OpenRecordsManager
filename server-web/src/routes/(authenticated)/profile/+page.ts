import { LocationController } from '#lib/api/index.js';
import { getApiClient } from '#lib/api-client.js';

export async function load({ parent }) {
	const data = await parent();

	const { data: actionsData, error: actionsError } = await LocationController.listLocationActions({
		client: getApiClient(),
		path: { id: data.me.id },
	});

	return {
		error: actionsError,
		actions: actionsData?.success ? actionsData.data : [],
	};
}
