import { AuthController, LocationTypeController } from '#lib/api/index.js';
import { getApiClient } from '#lib/api-client.js';

export async function load({ parent }) {
	const client = getApiClient();

	const [parentData, typesResult, providersResult] = await Promise.all([
		parent(),
		LocationTypeController.listLocationTypes({ client }),
		AuthController.retrieveAvailableAuthProviders({ client }),
	]);

	return {
		error: typesResult.error ?? providersResult.error,
		types: typesResult.data?.data ?? [],
		authProviders: providersResult.data?.data ?? [],
		auditCommentRequired: {
			create: parentData.auditPolicy.some(
				policy => policy.entityType === 'location' && policy.operation === 'CREATE' && policy.requiresComment
			),
			update: parentData.auditPolicy.some(
				policy => policy.entityType === 'location' && policy.operation === 'UPDATE' && policy.requiresComment
			),
		},
	};
}
