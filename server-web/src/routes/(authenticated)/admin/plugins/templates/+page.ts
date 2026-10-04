import { TemplateController, TemplateType } from '#lib/api/index.js';
import { getApiClient } from '#lib/api-client.js';
import { templateName } from '#lib/i18n/labels.js';

const TEMPLATE_TYPES: ReadonlyArray<string> = Object.values(TemplateType);

function parseTemplateType(value: string | null): TemplateType | undefined {
	return value && TEMPLATE_TYPES.includes(value) ? (value as TemplateType) : undefined;
}

export async function load({ url }: { url: URL }) {
	const type = parseTemplateType(url.searchParams.get('type'));

	const { data, error } = await TemplateController.listTemplates({
		client: getApiClient(),
		query: { type },
	});

	if (!data?.success) {
		return { type, templates: [], error };
	}

	const templates = data.data
		.map(template => ({
			type: parseTemplateType(template.type)!,
			id: template.id,
			name: templateName(template.type, template.id),
		}))
		.toSorted((a, b) => a.type.localeCompare(b.type) || a.name.localeCompare(b.name));

	return { type, templates };
}
