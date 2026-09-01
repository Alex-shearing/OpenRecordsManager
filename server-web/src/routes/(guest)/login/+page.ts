import { AuthController } from '$lib/api';

export async function load() {
	const { data, error } = await AuthController.getAll();

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
