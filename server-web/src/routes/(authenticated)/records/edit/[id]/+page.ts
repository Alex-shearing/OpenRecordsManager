import { RecordController } from '#lib/api/index.js';
import { getApiClient } from '#lib/api-client.js';

export async function load({ params, parent }) {
	const [layout, result] = await Promise.all([
		parent(),
		RecordController.getRecord({
			client: getApiClient(),
			path: { id: params.id },
		}),
	]);

	if (result.error) {
		return {
			record: undefined,
			loadError: result.error ?? layout.error,
		};
	}

	return {
		record: result.data.data,
		loadError: layout.error,
	};
}
