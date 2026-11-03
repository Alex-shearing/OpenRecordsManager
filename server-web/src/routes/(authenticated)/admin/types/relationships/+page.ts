import {
	AuditEntityType,
	AuditOperation,
	LocationTypeController,
	RecordTypeController,
	RelationshipTypeController,
} from '#lib/api/index.js';
import { getApiClient } from '#lib/api-client.js';
import { isAuditCommentRequired } from '#lib/audit/audit-utils.js';

export async function load({ parent }) {
	const client = getApiClient();
	const [parentData, typesResult, recordTypesResult, locationTypesResult] = await Promise.all([
		parent(),
		RelationshipTypeController.listRelationshipTypes({ client }),
		RecordTypeController.getRecordTypes({ client }),
		LocationTypeController.listLocationTypes({ client }),
	]);

	return {
		error: typesResult.error ?? recordTypesResult.error ?? locationTypesResult.error,
		types: typesResult.data?.data ?? [],
		availableRecordTypes: recordTypesResult.data?.data ?? [],
		availableLocationTypes: locationTypesResult.data?.data ?? [],
		auditCommentRequired: {
			create: isAuditCommentRequired(
				parentData.auditPolicy,
				AuditEntityType.RELATIONSHIP_TYPE,
				AuditOperation.CREATE
			),
			update: isAuditCommentRequired(
				parentData.auditPolicy,
				AuditEntityType.RELATIONSHIP_TYPE,
				AuditOperation.UPDATE
			),
		},
	};
}
