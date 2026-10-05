import {
	AuditEntityType,
	AuditOperation,
	ObjectPropertyController,
	RecordTypeController,
	type RecordTypeResponse,
	type SimpleObjectPropertyResponse,
} from '#lib/api/index.js';
import { getApiClient } from '#lib/api-client.js';
import { isAuditCommentRequired } from '#lib/audit/audit-utils.js';

export async function load({ parent }) {
	const [parentData, typesResult, propertiesResult] = await Promise.all([
		parent(),
		RecordTypeController.getRecordTypes({ client: getApiClient() }),
		ObjectPropertyController.objectPropertyRetrieveAll({ client: getApiClient() }),
	]);

	return {
		error: typesResult.error ?? propertiesResult.error,
		types: typesResult.data?.data ?? [],
		properties: propertiesResult.data?.data ?? [],
		auditCommentRequired: {
			create: isAuditCommentRequired(parentData.auditPolicy, AuditEntityType.RECORD_TYPE, AuditOperation.CREATE),
			update: isAuditCommentRequired(parentData.auditPolicy, AuditEntityType.RECORD_TYPE, AuditOperation.UPDATE),
		},
	};
}
