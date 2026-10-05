import { AuditEntityType, AuditOperation, FileStoreController } from '#lib/api/index.js';
import { getApiClient } from '#lib/api-client.js';
import { isAuditCommentRequired } from '#lib/audit/audit-utils.js';

export async function load({ parent }) {
	const parentData = await parent();
	const client = getApiClient();

	const [storesResult, typesResult, middlewaresResult] = await Promise.all([
		FileStoreController.fileStoreRetrieveAll({ client }),
		FileStoreController.fileStoreTypeGet({ client }),
		FileStoreController.middlewareRetrieveAll({ client }),
	]);

	return {
		error: storesResult.error ?? typesResult.error ?? middlewaresResult.error,
		stores: storesResult.data?.success ? storesResult.data.data : [],
		types: typesResult.data?.success ? typesResult.data.data : [],
		middlewares: middlewaresResult.data?.success ? middlewaresResult.data.data : [],
		auditCommentRequired: {
			create: isAuditCommentRequired(
				parentData.auditPolicy,
				AuditEntityType.FILE_STORE,
				AuditOperation.CREATE
			),
			update: isAuditCommentRequired(
				parentData.auditPolicy,
				AuditEntityType.FILE_STORE,
				AuditOperation.UPDATE
			),
			delete: isAuditCommentRequired(
				parentData.auditPolicy,
				AuditEntityType.FILE_STORE,
				AuditOperation.DELETE
			),
		},
	};
}
