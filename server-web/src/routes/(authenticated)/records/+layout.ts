import { AuditEntityType, AuditOperation, RecordTypeController } from '#lib/api/index.js';
import { getApiClient } from '#lib/api-client.js';
import { isAuditCommentRequired } from '#lib/audit/audit-utils.js';

export async function load({ parent }) {
	const [parentData, { data, error }] = await Promise.all([
		parent(),
		RecordTypeController.getRecordTypes({
			client: getApiClient(),
		}),
	]);

	return {
		error,
		types: data?.data ?? [],
		auditCommentRequired: {
			create: isAuditCommentRequired(parentData.auditPolicy, AuditEntityType.RECORD, AuditOperation.CREATE),
			update: isAuditCommentRequired(parentData.auditPolicy, AuditEntityType.RECORD, AuditOperation.UPDATE),
			createRevision: isAuditCommentRequired(
				parentData.auditPolicy,
				AuditEntityType.RECORD_REVISION,
				AuditOperation.CREATE
			),
		},
	};
}
