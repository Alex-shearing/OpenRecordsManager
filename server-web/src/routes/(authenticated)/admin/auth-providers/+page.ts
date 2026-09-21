import { AuthController } from '$lib/api';
import { getApiClient } from '$lib/api-client';

export async function load({ parent }) {
	const parentData = await parent();
	const client = getApiClient();

	const [providersResult, typesResult] = await Promise.all([
		AuthController.retrieveAllAuthProviders({ client }),
		AuthController.retrieveAuthProviderTypes({ client }),
	]);

	const loadError = providersResult.error?.error || typesResult.error?.error;

	return {
		error: loadError,
		providers: providersResult.data?.success ? providersResult.data.data : [],
		types: typesResult.data?.success ? typesResult.data.data : [],
		auditCommentRequired: {
			create: parentData.auditPolicy.some(
				policy => policy.entityType === 'auth_provider' && policy.operation === 'CREATE' && policy.requiresComment
			),
			update: parentData.auditPolicy.some(
				policy => policy.entityType === 'auth_provider' && policy.operation === 'UPDATE' && policy.requiresComment
			),
		},
	};
}
