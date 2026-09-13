import { FileStoreController } from '$lib/api';
import { getApiClient } from '$lib/api-client';

export async function load({ parent }) {
	const parentData = await parent();
	const client = getApiClient();

	const [middlewaresResult, typesResult] = await Promise.all([
		FileStoreController.middlewareRetrieveAll({ client }),
		FileStoreController.fileStoreMiddlewareTypeGet({ client }),
	]);

	return {
		error: middlewaresResult.error?.error || typesResult.error?.error,
		middlewares: middlewaresResult.data?.success ? middlewaresResult.data.data : [],
		types: typesResult.data?.success ? typesResult.data.data : [],
		auditCommentRequired: {
			create: parentData.auditPolicy.some(
				policy =>
					policy.entityType === 'file_store_middleware' && policy.operation === 'CREATE' && policy.requiresComment
			),
			update: parentData.auditPolicy.some(
				policy =>
					policy.entityType === 'file_store_middleware' && policy.operation === 'UPDATE' && policy.requiresComment
			),
			delete: parentData.auditPolicy.some(
				policy =>
					policy.entityType === 'file_store_middleware' && policy.operation === 'DELETE' && policy.requiresComment
			),
		},
	};
}
