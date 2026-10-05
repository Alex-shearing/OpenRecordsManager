import { AuditEntityType, AuditOperation, AuthController } from '#lib/api/index.js';
import { getApiClient } from '#lib/api-client.js';
import { isAuditCommentRequired } from '#lib/audit/audit-utils.js';

export async function load({ parent }) {
	const parentData = await parent();
	const client = getApiClient();

	const [providersResult, typesResult] = await Promise.all([
		AuthController.retrieveAllAuthProviders({ client }),
		AuthController.retrieveAuthProviderTypes({ client }),
	]);

	return {
		error: providersResult.error ?? typesResult.error,
		providers: providersResult.data?.success ? providersResult.data.data : [],
		types: typesResult.data?.success ? typesResult.data.data : [],
		auditCommentRequired: {
			create: isAuditCommentRequired(
				parentData.auditPolicy,
				AuditEntityType.AUTH_PROVIDER,
				AuditOperation.CREATE
			),
			update: isAuditCommentRequired(
				parentData.auditPolicy,
				AuditEntityType.AUTH_PROVIDER,
				AuditOperation.UPDATE
			),
		},
	};
}
