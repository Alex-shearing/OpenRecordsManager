import { AuditController } from '$lib/api';
import { getApiClient } from '$lib/api-client';

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
