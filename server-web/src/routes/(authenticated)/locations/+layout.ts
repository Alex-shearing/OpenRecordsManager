import { AuthController, LocationTypeController } from '$lib/api';
import { getApiClient } from '$lib/api-client';

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
			user: {
				create: parentData.auditPolicy.some(
					policy => policy.entityType === 'user' && policy.operation === 'CREATE' && policy.requiresComment
				),
				update: parentData.auditPolicy.some(
					policy => policy.entityType === 'user' && policy.operation === 'UPDATE' && policy.requiresComment
				),
			},
			group: {
				create: parentData.auditPolicy.some(
					policy => policy.entityType === 'group' && policy.operation === 'CREATE' && policy.requiresComment
				),
				update: parentData.auditPolicy.some(
					policy => policy.entityType === 'group' && policy.operation === 'UPDATE' && policy.requiresComment
				),
			},
		},
	};
}
