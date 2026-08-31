import type { ConfigTypeResponse } from '$lib/api/types.gen';

export type ConfigGroup = {
	id: string;
	title: string;
	prefix: string;
};

export const CONFIG_GROUPS: ConfigGroup[] = [
	{ id: 'workgroup', title: 'Workgroup', prefix: 'workgroup.' },
	{ id: 'debug', title: 'Debugging', prefix: 'app.debug.' },
	{ id: 'database', title: 'Database', prefix: 'app.database.' },
	{ id: 'security', title: 'Security', prefix: 'app.security.' },
	{ id: 'web', title: 'Web branding', prefix: 'app.web.' },
	{ id: 'audit', title: 'Audit', prefix: 'app.audit.' }
];

export function groupConfigs(configs: ConfigTypeResponse[]): Array<{ group: ConfigGroup | null; items: ConfigTypeResponse[] }> {
	const sorted = [...configs].sort((a, b) => a.key.localeCompare(b.key));
	const grouped = new Map<string, ConfigTypeResponse[]>();

	for (const config of sorted) {
		const match = CONFIG_GROUPS.find((group) => config.key.startsWith(group.prefix));
		const groupId = match?.id ?? 'other';
		const bucket = grouped.get(groupId) ?? [];
		bucket.push(config);
		grouped.set(groupId, bucket);
	}

	const sections: Array<{ group: ConfigGroup | null; items: ConfigTypeResponse[] }> = [];

	for (const group of CONFIG_GROUPS) {
		const items = grouped.get(group.id);
		if (items?.length) {
			sections.push({ group, items });
		}
	}

	const other = grouped.get('other');
	if (other?.length) {
		sections.push({ group: null, items: other });
	}

	return sections;
}

export function formatConfigValueForInput(config: ConfigTypeResponse): string {
	const value = config.currentValue ?? config.defaultValue;

	switch (config.type) {
		case 'BOOL':
			return value === true || value === 'true' ? 'true' : 'false';
		case 'STRING_LIST':
		case 'INT_LIST':
			if (Array.isArray(value)) {
				return value.map(String).join('\n');
			}
			if (typeof value === 'string') {
				return value
					.split(';')
					.map((entry) => entry.trim())
					.filter(Boolean)
					.join('\n');
			}
			return '';
		case 'INT':
		case 'DOUBLE':
			return value == null || value === '' ? '' : String(value);
		default:
			return value == null ? '' : String(value);
	}
}

export function serializeConfigValue(type: ConfigTypeResponse['type'], input: string): string {
	switch (type) {
		case 'BOOL':
			return input === 'true' ? 'true' : 'false';
		case 'STRING_LIST':
		case 'INT_LIST':
			return input
				.split(/\r?\n/)
				.map((entry) => entry.trim())
				.filter(Boolean)
				.join(';');
		default:
			return input.trim();
	}
}

export function formatConfigValueForDisplay(value: unknown, type: ConfigTypeResponse['type']): string {
	if (value == null || value === '') {
		return '—';
	}

	if (type === 'STRING_LIST' || type === 'INT_LIST') {
		if (Array.isArray(value)) {
			return value.map(String).join(', ');
		}
		if (typeof value === 'string') {
			return value.split(';').join(', ');
		}
	}

	return String(value);
}

export function buildSavedValues(configs: ConfigTypeResponse[]): Record<string, string> {
	return Object.fromEntries(configs.map((config) => [config.key, formatConfigValueForInput(config)]));
}

export function findChangedConfigs(
	configs: ConfigTypeResponse[],
	draftValues: Record<string, string>,
	savedValues: Record<string, string>
): ConfigTypeResponse[] {
	return configs.filter(
		(config) =>
			!configValuesEqual(
				config.type,
				draftValues[config.key] ?? '',
				savedValues[config.key] ?? ''
			)
	);
}

export function configValuesEqual(
	type: ConfigTypeResponse['type'],
	left: string,
	right: string
): boolean {
	return serializeConfigValue(type, left) === serializeConfigValue(type, right);
}
