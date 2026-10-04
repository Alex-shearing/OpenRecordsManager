import { AuthController } from '#lib/api/index.js';
import { getApiClient } from '#lib/api-client.js';

export async function load() {
	const { data, error } = await AuthController.retrieveAvailableAuthProviders({ client: getApiClient() });

	if (error) {
		return {
			inputProviders: [],
			redirectProviders: [],
			providersError: error,
		};
	}

	const providers = data.data;

	return {
		inputProviders: providers.filter(provider => provider.loginSchema),
		redirectProviders: providers.filter(provider => !provider.loginSchema),
	};
}
