import { TemplateController } from '$lib/api';
import { getApiClient } from '$lib/api-client';
import { templateName } from '$lib/i18n/labels';

export async function load() {
	const client = getApiClient();
	const typesResult = await TemplateController.getTemplateTypes({ client });

	if (!typesResult.data?.success) {
		return {
			sections: [],
			error: typesResult.error,
		};
	}

	const types = typesResult.data.data.toSorted((a, b) => a.localeCompare(b));
	const sections = await Promise.all(
		types.map(async type => {
			const templatesResult = await TemplateController.getTemplatesForType({
				client,
				path: { type },
			});

			return {
				error: templatesResult.error,
				type,
				templates: templatesResult.data?.success
					? [...(templatesResult.data.data ?? [])].sort((a, b) =>
							templateName(type, a).localeCompare(templateName(type, b))
						)
					: [],
			};
		})
	);

	return { sections };
}
