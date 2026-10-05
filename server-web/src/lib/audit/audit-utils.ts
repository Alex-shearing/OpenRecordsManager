import type { AuditPolicyResponse } from '#lib/api/index.js';
import { t } from '#lib/i18n/catalog.js';
import { formatDateTime } from '#lib/properties/formatValue.js';

export function formatEntityType(key: string) {
	return key
		.split('_')
		.map(part => part.charAt(0).toUpperCase() + part.slice(1))
		.join(' ');
}

export function groupPoliciesByEntity(policies: AuditPolicyResponse[]) {
	const grouped = new Map<string, Map<string, AuditPolicyResponse>>();

	for (const policy of policies) {
		const entityType = policy.entityType ?? '';
		const operation = policy.operation ?? '';
		if (!entityType || !operation) {
			continue;
		}

		let operations = grouped.get(entityType);
		if (!operations) {
			operations = new Map();
			grouped.set(entityType, operations);
		}
		operations.set(operation, policy);
	}

	return [...grouped.entries()].sort(([left], [right]) => left.localeCompare(right));
}

export function policyKey(entityType: string, operation: string) {
	return `${entityType}:${operation}`;
}

export function buildPolicyDraft(policies: AuditPolicyResponse[]) {
	return Object.fromEntries(
		policies.map(policy => [
			policyKey(policy.entityType ?? '', policy.operation ?? ''),
			{
				enabled: policy.enabled ?? false,
				requiresComment: policy.requiresComment ?? false,
			},
		])
	);
}

export function findChangedPolicies(
	policies: AuditPolicyResponse[],
	draft: Record<string, { enabled: boolean; requiresComment: boolean }>
) {
	return policies.filter(policy => {
		const key = policyKey(policy.entityType ?? '', policy.operation ?? '');
		const current = draft[key];
		if (!current) {
			return false;
		}
		return (
			current.enabled !== (policy.enabled ?? false) || current.requiresComment !== (policy.requiresComment ?? false)
		);
	});
}

export function formatInstant(value: string | undefined) {
	if (!value) {
		return t('web.common.em_dash');
	}
	return formatDateTime(value) ?? value;
}
