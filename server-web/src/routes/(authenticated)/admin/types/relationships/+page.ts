import { AuditEntityType, AuditOperation, LocationRelationshipTypeController } from '#lib/api/index.js';
import { getApiClient } from '#lib/api-client.js';
import { isAuditCommentRequired } from '#lib/audit/audit-utils.js';

export async function load({ parent }) {
	const [parentData, result] = await Promise.all([
		parent(),
		LocationRelationshipTypeController.listLocationRelationshipTypes({
			client: getApiClient(),
		}),
	]);

	return {
		error: result.error,
		types: result.data?.data ?? [],
		auditCommentRequired: {
			create: isAuditCommentRequired(
				parentData.auditPolicy,
				AuditEntityType.LOCATION_RELATIONSHIP_TYPE,
				AuditOperation.CREATE
			),
			update: isAuditCommentRequired(
				parentData.auditPolicy,
				AuditEntityType.LOCATION_RELATIONSHIP_TYPE,
				AuditOperation.UPDATE
			),
		},
	};
}
