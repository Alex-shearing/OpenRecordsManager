import { AuditController } from '#lib/api/index.js';
import { getApiClient } from '#lib/api-client.js';

export async function load() {
	const client = getApiClient();
	const [statusResult, policiesResult] = await Promise.all([
		AuditController.getAuditStatus({ client }),
		AuditController.listAuditPolicies({ client }),
	]);

	return {
		error: statusResult.error ?? policiesResult.error,
		status: statusResult.data?.success ? statusResult.data.data : null,
		policies: policiesResult.data?.success ? policiesResult.data.data : [],
	};
}
