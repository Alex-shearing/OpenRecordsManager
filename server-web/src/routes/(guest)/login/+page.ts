import { AuthController } from '$lib/api';
import { getApiClient } from '$lib/api-client';

export async function load() {
	const { data, error } = await AuthController.getAll({ client: getApiClient() });

	if (error || !data?.success) {
		return {
			inputProviders: [],
			redirectProviders: [],
			providersError: 'Failed to load login options.',
		};
	}

	const providers = data.data;

	return {
		inputProviders: providers.filter(provider => provider.loginSchema),
		redirectProviders: providers.filter(provider => !provider.loginSchema),
		providersError: null,
	};
}
