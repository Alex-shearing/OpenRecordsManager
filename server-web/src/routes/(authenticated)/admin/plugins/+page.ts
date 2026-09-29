import { PluginController } from '$lib/api';
import { getApiClient } from '$lib/api-client';
import { t } from '$lib/i18n/catalog';

export async function load({ parent }) {
	const parentData = await parent();
	const result = await PluginController.listPlugins({
		client: getApiClient(),
		query: { includeDisabled: true },
	});

	const auditPolicy = parentData.auditPolicy;

	return {
		plugins: result.data?.success ? result.data.data : [],
		auditCommentRequired: {
			create: auditPolicy.some(
				policy => policy.entityType === 'plugin' && policy.operation === 'CREATE' && policy.requiresComment
			),
			update: auditPolicy.some(
				policy => policy.entityType === 'plugin' && policy.operation === 'UPDATE' && policy.requiresComment
			),
			delete: auditPolicy.some(
				policy => policy.entityType === 'plugin' && policy.operation === 'DELETE' && policy.requiresComment
			),
		},
		error: result.error ? t('web.plugins.load_failed') : null,
	};
}
