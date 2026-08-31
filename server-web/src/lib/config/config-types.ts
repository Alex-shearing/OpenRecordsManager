import type { ConfigTypeResponse as GeneratedConfigTypeResponse } from '$lib/api/types.gen';

export type ConfigValueType = GeneratedConfigTypeResponse['type'];

type ConfigTypeBase = {
	key: string;
	name: string;
	description: string;
};

export type ConfigTypeStringResponse = ConfigTypeBase & {
	type: 'string';
	currentValue?: string;
	defaultValue?: string;
};

export type ConfigTypeBooleanResponse = ConfigTypeBase & {
	type: 'boolean';
	currentValue?: boolean;
	defaultValue?: boolean;
};

export type ConfigTypeNumberResponse = ConfigTypeBase & {
	type: 'number';
	currentValue?: number;
	defaultValue?: number;
};

export type ConfigTypeDecimalResponse = ConfigTypeBase & {
	type: 'decimal';
	currentValue?: number;
	defaultValue?: number;
};

export type ConfigTypeUuidResponse = ConfigTypeBase & {
	type: 'uuid';
	currentValue?: string;
	defaultValue?: string;
};

export type ConfigTypeStringListResponse = ConfigTypeBase & {
	type: 'string_list';
	currentValue?: string[];
	defaultValue?: string[];
};

export type ConfigTypeIntListResponse = ConfigTypeBase & {
	type: 'int_list';
	currentValue?: number[];
	defaultValue?: number[];
};

export type ConfigTypeObjectResponse = ConfigTypeBase & {
	type: 'object';
	currentValue?: unknown;
	defaultValue?: unknown;
};

/** Discriminated union of configuration responses by `type`. */
export type DescriminatedConfigTypeResponse =
	| ConfigTypeStringResponse
	| ConfigTypeBooleanResponse
	| ConfigTypeNumberResponse
	| ConfigTypeDecimalResponse
	| ConfigTypeUuidResponse
	| ConfigTypeStringListResponse
	| ConfigTypeIntListResponse
	| ConfigTypeObjectResponse;

export type ConfigDraftValue = string | boolean | number | null | string[] | number[];

export type ConfigDraftValues = Record<string, ConfigDraftValue>;
