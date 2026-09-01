import { TemplateController } from '$lib/api';
import { getApiClient } from '$lib/api-client';

export async function load() {
	const client = getApiClient();
	const typesResult = await TemplateController.getTemplateTypes({ client });

	if (!typesResult.data?.success) {
		return {
			sections: [],
			error: typesResult.error?.error ?? 'Failed to load template types.',
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
					? [...(templatesResult.data.data ?? [])].sort((a, b) => a.name.localeCompare(b.name))
					: [],
				error: templatesResult.error?.error ?? null,
			};
		})
	);

	return { sections, error: null };
}
