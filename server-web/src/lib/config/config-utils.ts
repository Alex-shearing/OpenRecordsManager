import type {
	ConfigDraftValue,
	ConfigDraftValues,
	DescriminatedConfigTypeResponse,
	ConfigValueType
} from '$lib/config/config-types';

export type { ConfigDraftValue, ConfigDraftValues } from '$lib/config/config-types';

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

export function groupConfigs(
	configs: DescriminatedConfigTypeResponse[]
): Array<{ group: ConfigGroup | null; items: DescriminatedConfigTypeResponse[] }> {
	const sorted = [...configs].sort((a, b) => a.key.localeCompare(b.key));
	const grouped = new Map<string, DescriminatedConfigTypeResponse[]>();

	for (const config of sorted) {
		const match = CONFIG_GROUPS.find((group) => config.key.startsWith(group.prefix));
		const groupId = match?.id ?? 'other';
		const bucket = grouped.get(groupId) ?? [];
		bucket.push(config);
		grouped.set(groupId, bucket);
	}

	const sections: Array<{ group: ConfigGroup | null; items: DescriminatedConfigTypeResponse[] }> =
		[];

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

export function parseConfigDraftValue(config: DescriminatedConfigTypeResponse): ConfigDraftValue {
	const value = config.currentValue ?? config.defaultValue;

	switch (config.type) {
		case 'boolean':
			return value === true || value === 'true';
		case 'string_list':
			if (Array.isArray(value)) {
				return value.length > 0 ? value.map(String) : [''];
			}
			return [''];
		case 'int_list':
			if (Array.isArray(value)) {
				return value.map((entry) => Number(entry)).filter((entry) => !Number.isNaN(entry));
			}
			return [];
		case 'number':
			return typeof value === 'number' && !Number.isNaN(value) ? value : null;
		case 'decimal':
			return typeof value === 'number' && !Number.isNaN(value) ? value : null;
		default:
			return value == null ? '' : String(value);
	}
}

export function serializeConfigDraftValue(type: ConfigValueType, value: ConfigDraftValue): any {
	switch (type) {
		case 'boolean':
			return value;
		case 'string_list':
			return (value as string[]).map((entry) => entry.trim()).filter(Boolean);
		case 'int_list':
			return (value as number[]).filter((entry) => !Number.isNaN(entry));
		case 'number':
		case 'decimal':
			return value === null || value === '' ? null : value;
		default:
			return typeof value === 'string' ? value.trim() : value;
	}
}

export function formatConfigValueForDisplay(value: unknown, type: ConfigValueType): string {
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

export function buildSavedValues(configs: DescriminatedConfigTypeResponse[]): ConfigDraftValues {
	return Object.fromEntries(configs.map((config) => [config.key, parseConfigDraftValue(config)]));
}

export function configDraftValuesEqual(
	type: ConfigValueType,
	left: ConfigDraftValue,
	right: ConfigDraftValue
): boolean {
	return (
		JSON.stringify(serializeConfigDraftValue(type, left)) ===
		JSON.stringify(serializeConfigDraftValue(type, right))
	);
}

export function findChangedConfigs(
	configs: DescriminatedConfigTypeResponse[],
	draftValues: ConfigDraftValues,
	savedValues: ConfigDraftValues
): DescriminatedConfigTypeResponse[] {
	return configs.filter(
		(config) =>
			!configDraftValuesEqual(config.type, draftValues[config.key], savedValues[config.key])
	);
}
