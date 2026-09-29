import { AuthController } from '$lib/api';
import { getApiClient } from '$lib/api-client';
import { t } from '$lib/i18n/catalog';

export async function load() {
	const { data, error } = await AuthController.retrieveAvailableAuthProviders({ client: getApiClient() });

	if (error || !data?.success) {
		return {
			inputProviders: [],
			redirectProviders: [],
			providersError: t('web.login.load_failed'),
		};
	}

	const providers = data.data;

	return {
		inputProviders: providers.filter(provider => provider.loginSchema),
		redirectProviders: providers.filter(provider => !provider.loginSchema),
		providersError: null,
	};
}
