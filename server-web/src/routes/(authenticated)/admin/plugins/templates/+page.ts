import { TemplateController } from '$lib/api';
import { getApiClient } from '$lib/api-client';
import { templateName } from '$lib/i18n/labels';

export async function load() {
	const { data, error } = await TemplateController.listTemplates({
		client: getApiClient(),
	});

	if (!data?.success) {
		return { templates: [], error };
	}

	const templates = data.data
		.map(template => ({
			type: template.type,
			id: template.id,
			name: templateName(template.type, template.id),
		}))
		.toSorted((a, b) => a.type.localeCompare(b.type) || a.name.localeCompare(b.name));

	return { templates };
}
