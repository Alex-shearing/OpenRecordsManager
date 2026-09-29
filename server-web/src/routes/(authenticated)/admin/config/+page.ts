import { ConfigController } from '$lib/api';
import { getApiClient } from '$lib/api-client';
import { t } from '$lib/i18n/catalog';

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
		configs,
		requiresAuditComment,
		error: configResult.error ? t('web.config.load_failed') : null,
	};
}
