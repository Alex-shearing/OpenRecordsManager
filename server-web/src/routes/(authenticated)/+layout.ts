import { ObjectPropertyController, UserController } from '$lib';

export async function load() {
	const { data: meResp } = await UserController.me();
	const me = meResp?.data || {
		id: '00000000-0000-0000-0000-000000000000',
		username: 'Me',
		properties: {}
	};
	const { data: propertiesResp } = await ObjectPropertyController.objectPropertyRetrieveAll();
	const properties = propertiesResp?.data || [];

	return {
		me,
		properties
	};
}
