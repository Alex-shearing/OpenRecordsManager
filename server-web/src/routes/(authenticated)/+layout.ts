import { goto } from '$app/navigation';
import { AuditController, ObjectPropertyController, UserController } from '$lib/api';
import { getApiClient } from '$lib/api-client';

export async function load({ url }) {
	const client = getApiClient();
	const [meR, propertiesR, auditPolicyR] = await Promise.all([
		UserController.me({ client }),
		ObjectPropertyController.objectPropertyRetrieveAll({ client }),
		AuditController.listPolicies({ client }),
	]);

	if (meR.error && meR.response.status === 401) {
		const dest = new URL('/login', document.baseURI);
		dest.searchParams.append('redirect', url.pathname.substring(1) + url.search);

		throw goto(dest);
	}

	const me = meR.data?.data || {
		id: '00000000-0000-0000-0000-000000000000',
		username: 'Me',
		properties: {},
	};
	const properties = propertiesR.data?.data || [];
	const auditPolicy = auditPolicyR.data?.data || [];

	return {
		me,
		properties,
		auditPolicy,
	};
}
