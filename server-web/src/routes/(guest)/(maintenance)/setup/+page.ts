import { DatabaseController } from '$lib/api';

export async function load() {
	return {
		status: await DatabaseController.upgrade(),
	};
}
