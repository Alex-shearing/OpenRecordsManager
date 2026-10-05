import {
	AuditEntityType,
	AuditOperation,
	AuthController,
	LocationTypeController,
} from '#lib/api/index.js';
import { getApiClient } from '#lib/api-client.js';
import { isAuditCommentRequired } from '#lib/audit/audit-utils.js';

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
			create: isAuditCommentRequired(
				parentData.auditPolicy,
				AuditEntityType.LOCATION,
				AuditOperation.CREATE
			),
			update: isAuditCommentRequired(
				parentData.auditPolicy,
				AuditEntityType.LOCATION,
				AuditOperation.UPDATE
			),
		},
	};
}
