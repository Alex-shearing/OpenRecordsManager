import { AuditController } from '$lib/api';
import { getApiClient } from '$lib/api-client';
import { t } from '$lib/i18n/catalog';

export async function load() {
	const client = getApiClient();
	const [statusResult, policiesResult] = await Promise.all([
		AuditController.getAuditStatus({ client }),
		AuditController.listAuditPolicies({ client }),
	]);

	return {
		status: statusResult.data?.success ? statusResult.data.data : null,
		policies: policiesResult.data?.success ? policiesResult.data.data : [],
		error: statusResult.error || policiesResult.error ? t('web.audit.load_failed') : null,
	};
}
