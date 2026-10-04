import { ConfigController } from '#lib/api/index.js';
import { getApiClient } from '#lib/api-client.js';

export async function load({ parent }) {
	const [parentData, configResult] = await Promise.all([
		parent(),
		ConfigController.getAllConfig({ client: getApiClient() }),
	]);

	const configs = configResult.data?.success ? configResult.data.data : [];
	const requiresAuditComment = parentData.auditPolicy.some(
		policy => policy.entityType === 'config' && policy.operation === 'UPDATE' && policy.requiresComment
	);

	return {
		error: configResult.error,
		configs,
		requiresAuditComment,
	};
}
