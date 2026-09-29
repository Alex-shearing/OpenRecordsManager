import type { ConfigTypeResponse } from '$lib/api/types.gen';
import { t } from '$lib/i18n/catalog';
import type { DistributivePick } from '$lib/util-types';

export type EnsureCurrentValue<T> = T extends any
	? Omit<T, 'currentValue'> & { currentValue: NonNullable<T[keyof T & 'currentValue']> }
	: never;

export type ConfigDraftValue = EnsureCurrentValue<DistributivePick<ConfigTypeResponse, 'currentValue' | 'type'>>;

export type ConfigDraftValueType = ConfigDraftValue['currentValue'];
export type ConfigValueType = ConfigDraftValue['type'];

export const CONFIG_GROUPS = [
	{ id: 'workgroup', titleKey: 'web.config.group.workgroup', prefix: 'workgroup.' },
	{ id: 'web', titleKey: 'web.config.group.web', prefix: 'app.web.' },
	{ id: 'database', titleKey: 'web.config.group.database', prefix: 'app.database.' },
	{ id: 'security', titleKey: 'web.config.group.security', prefix: 'app.security.' },
	{ id: 'audit', titleKey: 'web.config.group.audit', prefix: 'app.audit.' },
	{ id: 'plugins', titleKey: 'web.config.group.plugins', prefix: 'app.plugins.' },
	{ id: 'debug', titleKey: 'web.config.group.debug', prefix: 'app.debug.' },
	{ id: 'other', titleKey: 'web.config.group.other', prefix: '' },
];

export type ConfigGroup = (typeof CONFIG_GROUPS)[0];

export function groupConfigs(configs: ConfigTypeResponse[]) {
	const sorted = [...configs].sort((a, b) => a.key.localeCompare(b.key));
	const grouped = new Map<string, ConfigTypeResponse[]>();

	for (const config of sorted) {
		const match = CONFIG_GROUPS.find(group => config.key.startsWith(group.prefix));
		const groupId = match?.id ?? 'other';
		const bucket = grouped.get(groupId) ?? [];
		bucket.push(config);
		grouped.set(groupId, bucket);
	}

	const sections: { group: ConfigGroup; items: ConfigTypeResponse[] }[] = [];

	for (const group of CONFIG_GROUPS) {
		const items = grouped.get(group.id);
		if (items?.length) {
			sections.push({ group, items });
		}
	}

	return sections;
}

const DEFAULT_FALLBACKS: Record<ConfigValueType, any> = {
	boolean: false,
	string_list: [''],
	int_list: [],
	number: 0,
	decimal: 0,
	string: '',
	uuid: '',
	object: {},
};

export function parseConfigDraftValue(config: ConfigTypeResponse): ConfigDraftValue {
	return {
		type: config.type,
		currentValue: config.currentValue ?? config.defaultValue ?? DEFAULT_FALLBACKS[config.type],
	};
}

export function formatDefaultValueForDisplay(cfg: ConfigTypeResponse) {
	if (cfg.defaultValue == null || cfg.defaultValue === '') {
		return t('web.common.em_dash');
	}

	if (cfg.type === 'string_list' || cfg.type === 'int_list') {
		return cfg.defaultValue.map(String).join(', ');
	}

	return String(cfg.defaultValue);
}

export function buildSavedValues(configs: ConfigTypeResponse[]): Record<string, ConfigDraftValue> {
	return Object.fromEntries(configs.map(config => [config.key, parseConfigDraftValue(config)]));
}

export function findChangedConfigs(
	configs: ConfigTypeResponse[],
	draftValues: Record<string, ConfigDraftValue>,
	savedValues: Record<string, ConfigDraftValue>
): ConfigTypeResponse[] {
	return configs.filter(
		config =>
			JSON.stringify(draftValues[config.key].currentValue) !== JSON.stringify(savedValues[config.key].currentValue)
	);
}
