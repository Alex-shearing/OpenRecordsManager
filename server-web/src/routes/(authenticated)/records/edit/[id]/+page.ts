import { RecordController } from '$lib/api';
import { getApiClient } from '$lib/api-client';

export async function load({ params, parent }) {
	const [layout, result] = await Promise.all([
		parent(),
		RecordController.get1({
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
