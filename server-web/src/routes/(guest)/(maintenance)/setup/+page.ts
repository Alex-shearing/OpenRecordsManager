import { DatabaseController } from '#lib/api/index.js';
import { getApiClient } from '#lib/api-client.js';

export async function load() {
	return {
		status: await DatabaseController.getSchemaMigrationStatus({ client: getApiClient() }),
	};
}
