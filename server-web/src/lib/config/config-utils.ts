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
		case 'boolean':
			return value === true || value === 'true' ? 'true' : 'false';
		case 'string_list':
		case 'int_list':
			if (Array.isArray(value)) {
				return value.map(String).join('\n');
			}
			return '';
		case 'number':
		case 'decimal':
			return value == null || value === '' ? '' : String(value);
		default:
			return value == null ? '' : String(value);
	}
}

export function serializeConfigValue(type: ConfigTypeResponse['type'], input: string): unknown {
	switch (type) {
		case 'boolean':
			return input === 'true';
		case 'string_list':
			return input
				.split(/\r?\n/)
				.map((entry) => entry.trim())
				.filter(Boolean);
		case 'int_list':
			return input
				.split(/\r?\n/)
				.map((entry) => entry.trim())
				.filter(Boolean)
				.map(Number);
		case 'number':
			return input.trim() === '' ? null : Number.parseInt(input.trim(), 10);
		case 'decimal':
			return input.trim() === '' ? null : Number.parseFloat(input.trim());
		case 'uuid':
			return input.trim();
		default:
			return input.trim();
	}
}

export function formatConfigValueForDisplay(value: unknown, type: ConfigTypeResponse['type']): string {
	if (value == null || value === '') {
		return '—';
	}

	if (type === 'string_list' || type === 'int_list') {
		if (Array.isArray(value)) {
			return value.map(String).join(', ');
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
	return JSON.stringify(serializeConfigValue(type, left)) === JSON.stringify(serializeConfigValue(type, right));
}
