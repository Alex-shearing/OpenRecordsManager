import { AuditController, ConfigController } from '$lib';
import type { DescriminatedConfigTypeResponse } from '$lib/config/config-types';

export async function load() {
	const [configResult, policyResult] = await Promise.all([
		ConfigController.getAllConfig(),
		AuditController.listPolicies()
	]);

	const configs = configResult.data?.success
		? configResult.data.data.map((a) => a as DescriminatedConfigTypeResponse)
		: [];
	const requiresAuditComment =
		policyResult.data?.success &&
		policyResult.data.data.some(
			(policy) =>
				policy.entityType === 'config' && policy.operation === 'UPDATE' && policy.requiresComment
		);

	return {
		configs,
		requiresAuditComment: Boolean(requiresAuditComment),
		error: configResult.error ? 'Failed to load configuration settings.' : null
	};
}
