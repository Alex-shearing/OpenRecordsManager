import { AuditEntityType, AuditOperation, FileStoreController } from '#lib/api/index.js';
import { getApiClient } from '#lib/api-client.js';
import { isAuditCommentRequired } from '#lib/audit/audit-utils.js';

export async function load({ parent }) {
	const parentData = await parent();
	const client = getApiClient();

	const [middlewaresResult, typesResult] = await Promise.all([
		FileStoreController.middlewareRetrieveAll({ client }),
		FileStoreController.fileStoreMiddlewareTypeGet({ client }),
	]);

	return {
		error: middlewaresResult.error ?? typesResult.error,
		middlewares: middlewaresResult.data?.success ? middlewaresResult.data.data : [],
		types: typesResult.data?.success ? typesResult.data.data : [],
		auditCommentRequired: {
			create: isAuditCommentRequired(
				parentData.auditPolicy,
				AuditEntityType.FILE_STORE_MIDDLEWARE,
				AuditOperation.CREATE
			),
			update: isAuditCommentRequired(
				parentData.auditPolicy,
				AuditEntityType.FILE_STORE_MIDDLEWARE,
				AuditOperation.UPDATE
			),
			delete: isAuditCommentRequired(
				parentData.auditPolicy,
				AuditEntityType.FILE_STORE_MIDDLEWARE,
				AuditOperation.DELETE
			),
		},
	};
}
