export type DistributivePick<T, K extends keyof T> = T extends any
	? Pick<T, Extract<keyof T, K>>
	: never;
