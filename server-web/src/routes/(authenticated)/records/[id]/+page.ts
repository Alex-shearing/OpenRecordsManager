import { RecordController } from '#lib/api/index.js';
import { getApiClient } from '#lib/api-client.js';

export async function load({ params, parent }) {
	const [layout, result, actionsResult] = await Promise.all([
		parent(),
		RecordController.getRecord({
			client: getApiClient(),
			path: { id: params.id },
		}),
		RecordController.listRecordActions({
			client: getApiClient(),
			path: { id: params.id },
		}),
	]);

	if (result.error) {
		return {
			record: undefined,
			actions: [],
			loadError: result.error ?? layout.error,
		};
	}

	return {
		record: result.data.data,
		actions: actionsResult.data?.success ? actionsResult.data.data : [],
		loadError: layout.error,
	};
}
