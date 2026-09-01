import { AuditController, ObjectPropertyController, UserController } from '$lib';

export async function load() {
	const [meR, propertiesR, auditPolicyR] = await Promise.all([
		UserController.me(),
		ObjectPropertyController.objectPropertyRetrieveAll(),
		AuditController.listPolicies()
	]);

	const me = meR?.data?.data || {
		id: '00000000-0000-0000-0000-000000000000',
		username: 'Me',
		properties: {}
	};
	const properties = propertiesR?.data?.data || [];
	const auditPolicy = auditPolicyR?.data?.data || [];

	return {
		me,
		properties,
		auditPolicy
	};
}
