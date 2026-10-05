import { AuditEntityType, AuditOperation, ListController } from '#lib/api/index.js';
import { getApiClient } from '#lib/api-client.js';
import { isAuditCommentRequired } from '#lib/audit/audit-utils.js';

export async function load({ parent }) {
	const [parentData, result] = await Promise.all([parent(), ListController.getLists({ client: getApiClient() })]);

	return {
		error: result.error,
		listIds: result.data ? [...result.data.data].sort((a, b) => a.localeCompare(b)) : [],
		auditCommentRequired: {
			create: isAuditCommentRequired(parentData.auditPolicy, AuditEntityType.LIST, AuditOperation.CREATE),
			update: isAuditCommentRequired(parentData.auditPolicy, AuditEntityType.LIST, AuditOperation.UPDATE),
			createElement: isAuditCommentRequired(
				parentData.auditPolicy,
				AuditEntityType.LIST_ELEMENT,
				AuditOperation.CREATE
			),
			updateElement: isAuditCommentRequired(
				parentData.auditPolicy,
				AuditEntityType.LIST_ELEMENT,
				AuditOperation.UPDATE
			),
		},
	};
}
