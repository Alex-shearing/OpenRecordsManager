import { AuditEntityType, AuditOperation, PluginController } from '#lib/api/index.js';
import { getApiClient } from '#lib/api-client.js';
import { isAuditCommentRequired } from '#lib/audit/audit-utils.js';

export async function load({ parent }) {
	const parentData = await parent();
	const result = await PluginController.listPlugins({
		client: getApiClient(),
		query: { includeDisabled: true },
	});

	const auditPolicy = parentData.auditPolicy;

	return {
		error: result.error,
		plugins: result.data?.success ? result.data.data : [],
		auditCommentRequired: {
			create: isAuditCommentRequired(auditPolicy, AuditEntityType.PLUGIN, AuditOperation.CREATE),
			update: isAuditCommentRequired(auditPolicy, AuditEntityType.PLUGIN, AuditOperation.UPDATE),
			delete: isAuditCommentRequired(auditPolicy, AuditEntityType.PLUGIN, AuditOperation.DELETE),
		},
	};
}
