import { UserController } from '$lib/api';
import { getApiClient } from '$lib/api-client';
import { t } from '$lib/i18n/catalog';

export async function load({ parent }) {
	const data = await parent();

	const { data: actionsData, error: actionsError } = await UserController.listActions({
		client: getApiClient(),
		path: { id: data.me.id },
	});

	return {
		actions: actionsData?.success ? actionsData.data : [],
		error: actionsError ? t('web.profile.actions_load_failed') : null,
	};
}
