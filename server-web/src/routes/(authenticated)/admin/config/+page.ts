import { AuditEntityType, AuditOperation, ConfigController } from '#lib/api/index.js';
import { getApiClient } from '#lib/api-client.js';
import { isAuditCommentRequired } from '#lib/audit/audit-utils.js';

export async function load({ parent }) {
	const [parentData, configResult] = await Promise.all([
		parent(),
		ConfigController.getAllConfig({ client: getApiClient() }),
	]);

	const configs = configResult.data?.success ? configResult.data.data : [];

	return {
		error: configResult.error,
		configs,
		requiresAuditComment: isAuditCommentRequired(
			parentData.auditPolicy,
			AuditEntityType.CONFIG,
			AuditOperation.UPDATE
		),
	};
}
