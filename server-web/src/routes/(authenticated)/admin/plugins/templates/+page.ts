import { TemplateController } from '$lib/api';
import { getApiClient } from '$lib/api-client';
import { t } from '$lib/i18n/catalog';
import { templateName } from '$lib/i18n/labels';

export async function load() {
	const client = getApiClient();
	const typesResult = await TemplateController.getTemplateTypes({ client });

	if (!typesResult.data?.success) {
		return {
			sections: [],
			error: typesResult.error?.error ?? t('web.templates.load_types_failed'),
		};
	}

	const types = [...(typesResult.data.data ?? [])].sort((a, b) => a.localeCompare(b));
	const sections = await Promise.all(
		types.map(async type => {
			const templatesResult = await TemplateController.getTemplatesForType({
				client,
				path: { type },
			});

			return {
				type,
				templates: templatesResult.data?.success
					? [...(templatesResult.data.data ?? [])].sort((a, b) =>
							templateName(type, a).localeCompare(templateName(type, b))
						)
					: [],
				error: templatesResult.error?.error ?? null,
			};
		})
	);

	return { sections, error: null };
}
