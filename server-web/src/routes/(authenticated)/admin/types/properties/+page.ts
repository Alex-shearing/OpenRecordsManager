import {
	AuditEntityType,
	AuditOperation,
	ObjectPropertyController,
	type SimpleObjectPropertyResponse,
} from '#lib/api/index.js';
import { getApiClient } from '#lib/api-client.js';
import { isAuditCommentRequired } from '#lib/audit/audit-utils.js';

export async function load({ parent }) {
	const [parentData, result] = await Promise.all([
		parent(),
		ObjectPropertyController.objectPropertyRetrieveAll({ client: getApiClient() }),
	]);

	return {
		error: result.error,
		properties: result.data?.data ?? [],
		auditCommentRequired: {
			create: isAuditCommentRequired(parentData.auditPolicy, AuditEntityType.OBJECT_PROPERTY, AuditOperation.CREATE),
			update: isAuditCommentRequired(parentData.auditPolicy, AuditEntityType.OBJECT_PROPERTY, AuditOperation.UPDATE),
		},
	};
}
