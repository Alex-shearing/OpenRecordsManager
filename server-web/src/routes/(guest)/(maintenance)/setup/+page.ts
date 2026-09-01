import { DatabaseController } from '$lib/api';
import { getApiClient } from '$lib/api-client';

export async function load() {
	return {
		status: await DatabaseController.upgrade({ client: getApiClient() }),
	};
}
