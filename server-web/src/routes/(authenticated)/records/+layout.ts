import { RecordTypeController } from '$lib/api';
import { getApiClient } from '$lib/api-client';

export async function load({ parent }) {
	const [parentData, { data, error }] = await Promise.all([
		parent(),
		RecordTypeController.getRecordTypes({
			client: getApiClient(),
		}),
	]);

	return {
		error,
		types: data?.data ?? [],
		auditCommentRequired: {
			create: parentData.auditPolicy.some(
				policy => policy.entityType === 'record' && policy.operation === 'CREATE' && policy.requiresComment
			),
			update: parentData.auditPolicy.some(
				policy => policy.entityType === 'record' && policy.operation === 'UPDATE' && policy.requiresComment
			),
		},
	};
}
