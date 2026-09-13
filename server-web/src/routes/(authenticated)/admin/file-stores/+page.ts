import { FileStoreController } from '$lib/api';
import { getApiClient } from '$lib/api-client';

export async function load({ parent }) {
	const parentData = await parent();
	const client = getApiClient();

	const [storesResult, typesResult, middlewaresResult] = await Promise.all([
		FileStoreController.fileStoreRetrieveAll({ client }),
		FileStoreController.fileStoreTypeGet({ client }),
		FileStoreController.middlewareRetrieveAll({ client }),
	]);

	const loadError = storesResult.error?.error || typesResult.error?.error || middlewaresResult.error?.error;

	return {
		error: loadError,
		stores: storesResult.data?.success ? storesResult.data.data : [],
		types: typesResult.data?.success ? typesResult.data.data : [],
		middlewares: middlewaresResult.data?.success ? middlewaresResult.data.data : [],
		auditCommentRequired: {
			create: parentData.auditPolicy.some(
				policy => policy.entityType === 'file_store' && policy.operation === 'CREATE' && policy.requiresComment
			),
			update: parentData.auditPolicy.some(
				policy => policy.entityType === 'file_store' && policy.operation === 'UPDATE' && policy.requiresComment
			),
			delete: parentData.auditPolicy.some(
				policy => policy.entityType === 'file_store' && policy.operation === 'DELETE' && policy.requiresComment
			),
		},
	};
}
