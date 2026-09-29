import { t } from './catalog';

/** ResourceIdentifier {@code source:item} → key segment {@code source.item}. */
function idSegment(id: string): string {
	return id.replaceAll(':', '.');
}

/** Mirrors server ComponentType + TranslationField key conventions. */
export function objectPropertyName(id: string): string {
	return t(`object_property.${idSegment(id)}.name`);
}

export function objectPropertyDescription(id: string): string {
	return t(`object_property.${idSegment(id)}.description`);
}

export function recordTypeName(id: string): string {
	return t(`record_type.${idSegment(id)}.name`);
}

export function recordTypeDescription(id: string): string {
	return t(`record_type.${idSegment(id)}.description`);
}

export function listTypeName(id: string): string {
	return t(`list.${idSegment(id)}.name`);
}

export function listElementName(id: string): string {
	return t(`list_element.${idSegment(id)}.name`);
}

export function listElementDescription(id: string): string {
	return t(`list_element.${idSegment(id)}.description`);
}

export function pluginName(id: string): string {
	return t(`plugin.${id}.name`);
}

export function pluginDescription(id: string): string {
	return t(`plugin.${id}.description`);
}

export function configName(key: string): string {
	return t(`config.${key}.name`);
}

export function configDescription(key: string): string {
	return t(`config.${key}.description`);
}

export function userActionName(id: string): string {
	return t(`user_action.${idSegment(id)}.name`);
}

export function userActionDescription(id: string): string {
	return t(`user_action.${idSegment(id)}.description`);
}

export function recordActionName(id: string): string {
	return t(`record_action.${idSegment(id)}.name`);
}

export function recordActionDescription(id: string): string {
	return t(`record_action.${idSegment(id)}.description`);
}

/** Template list labels use the same keys as the registered vocabulary for that type. */
export function templateName(type: string, id: string): string {
	switch (type) {
		case 'object_property':
			return objectPropertyName(id);
		case 'list':
			return listTypeName(id);
		case 'list_element':
			return listElementName(id);
		case 'record_type':
			return recordTypeName(id);
		default:
			return t(`${type}.${idSegment(id)}.name`);
	}
}

export { t };
