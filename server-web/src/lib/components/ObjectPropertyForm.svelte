<script lang="ts">
	import type { ListElementResponse } from '#lib/api/types.gen.js';
	import { ListController } from '#lib/api/index.js';
	import { getApiClient } from '#lib/api-client.js';
	import type { SchemaFormError } from '#lib/components/SchemaForm.svelte';
	import TransferList from '#lib/components/TransferList.svelte';
	import ConfigIntListInput from '#lib/components/config/ConfigIntListInput.svelte';
	import ConfigStringListInput from '#lib/components/config/ConfigStringListInput.svelte';
	import { t, tApiErrorResponse } from '#lib/i18n/catalog.js';
	import { listElementName, objectPropertyDescription, objectPropertyName } from '#lib/i18n/labels.js';
	import type { TypePropertyAssignment } from '#lib/properties/createFields.js';
	import type { Snippet } from 'svelte';

	let {
		fields,
		values = $bindable({}),
		error = undefined,
		submitting = false,
		idPrefix = 'property',
		before,
		after,
	}: {
		fields: TypePropertyAssignment[];
		values?: Record<string, unknown>;
		error?: SchemaFormError;
		submitting?: boolean;
		idPrefix?: string;
		before?: Snippet;
		after?: Snippet;
	} = $props();

	const fieldErrors = $derived(error?.fieldErrors ?? {});

	let listCache = $state<Record<string, ListElementResponse[]>>({});
	let listLoading = $state<Record<string, boolean>>({});
	let listPending = $state<Record<string, boolean>>({});

	let jsonDrafts = $state<Record<string, string>>({});

	const listTypeIds = $derived.by(() => {
		const ids: string[] = [];
		for (const field of fields) {
			const type = field.property.type;
			const listType = field.property.listType;
			if ((type === 'list_item' || type === 'list_multiple') && listType && !ids.includes(listType)) {
				ids.push(listType);
			}
		}
		return ids;
	});

	$effect(() => {
		for (const listTypeId of listTypeIds) {
			if (listCache[listTypeId] !== undefined || listPending[listTypeId]) {
				continue;
			}
			void loadList(listTypeId);
		}
	});

	async function loadList(listTypeId: string) {
		listPending = { ...listPending, [listTypeId]: true };
		listLoading = { ...listLoading, [listTypeId]: true };

		try {
			const { data } = await ListController.getList({
				client: getApiClient(),
				path: { list: listTypeId },
			});
			listCache = {
				...listCache,
				[listTypeId]: data?.data?.elements ?? [],
			};
		} catch {
			listCache = { ...listCache, [listTypeId]: [] };
		} finally {
			listPending = { ...listPending, [listTypeId]: false };
			listLoading = { ...listLoading, [listTypeId]: false };
		}
	}

	function getValue(id: string): unknown {
		return values[id];
	}

	function setValue(id: string, next: unknown) {
		values = { ...values, [id]: next };
	}

	function isListElementActive(element: ListElementResponse, at = Date.now()): boolean {
		if (!element.activeTo) return true;
		const end = Date.parse(element.activeTo);
		return !Number.isNaN(end) && end > at;
	}

	/** Active elements, plus any currently selected values (so edits keep inactive selections visible). */
	function selectableListElements(
		elements: ListElementResponse[],
		selectedKeys: ReadonlyArray<string>
	): ListElementResponse[] {
		const selected = new Set(selectedKeys);
		return elements.filter(element => isListElementActive(element) || selected.has(element.type));
	}

	function asString(value: unknown): string {
		return typeof value === 'string' ? value : value == null ? '' : String(value);
	}

	function asStringArray(value: unknown): string[] {
		return Array.isArray(value) ? value.map(entry => String(entry)) : [];
	}

	function asNumberArray(value: unknown): number[] {
		if (!Array.isArray(value)) {
			return [];
		}
		return value.filter((entry): entry is number => typeof entry === 'number' && !Number.isNaN(entry));
	}

	function onNumberInput(id: string, raw: string) {
		if (raw.trim() === '') {
			setValue(id, null);
			return;
		}
		const parsed = Number(raw);
		setValue(id, Number.isNaN(parsed) ? null : parsed);
	}

	function toDatetimeLocal(value: unknown): string {
		if (typeof value !== 'string' || !value) {
			return '';
		}
		const date = new Date(value);
		if (Number.isNaN(date.getTime())) {
			return '';
		}
		const pad = (part: number) => String(part).padStart(2, '0');
		return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
	}

	function fromDatetimeLocal(raw: string): string | null {
		if (!raw) {
			return null;
		}
		const date = new Date(raw);
		return Number.isNaN(date.getTime()) ? null : date.toISOString();
	}

	function getJsonDraft(id: string): string {
		if (jsonDrafts[id] !== undefined) {
			return jsonDrafts[id];
		}
		const value = getValue(id);
		if (value === undefined || value === null) {
			return '';
		}
		try {
			return JSON.stringify(value, null, 2);
		} catch {
			return '';
		}
	}

	function onJsonInput(id: string, raw: string) {
		jsonDrafts = { ...jsonDrafts, [id]: raw };
		const trimmed = raw.trim();
		if (!trimmed) {
			setValue(id, null);
			return;
		}
		try {
			const parsed = JSON.parse(trimmed) as unknown;
			if (parsed !== null && typeof parsed === 'object') {
				setValue(id, parsed);
			}
		} catch {
			// Keep the draft while JSON is invalid; do not write into values.
		}
	}
</script>

<div class="flex flex-col gap-4">
	{@render before?.()}

	{#each fields as field (field.property.id)}
		{@const property = field.property}
		{@const id = property.id}
		{@const inputId = `${idPrefix}-${id}`}
		{@const errorId = `${inputId}-error`}
		{@const name = objectPropertyName(id)}
		{@const description = objectPropertyDescription(id)}
		{@const descriptionKey = `object_property.${id.replaceAll(':', '.')}.description`}
		{@const fieldError = fieldErrors[id]}
		{@const invalid = fieldError ? 'true' : undefined}
		{@const describedBy = fieldError ? errorId : undefined}

		{#if property.type === 'boolean'}
			<div class="flex flex-col gap-1">
				<label class="flex items-center gap-2">
					<input
						id={inputId}
						type="checkbox"
						name={id}
						class="size-4 rounded border-border-input"
						bind:checked={() => Boolean(getValue(id)), v => setValue(id, v)}
						disabled={submitting}
						aria-invalid={invalid}
						aria-describedby={describedBy}
					/>
					<span class="flex flex-col gap-0.5">
						<span>{name}</span>
						{#if description !== descriptionKey}
							<span class="text-hint">{description}</span>
						{/if}
					</span>
				</label>
				{#if fieldError}
					<span id={errorId} class="text-sm text-destructive" role="alert">
						{tApiErrorResponse(fieldError)}
					</span>
				{/if}
			</div>
		{:else if property.type === 'list_multiple' || property.type === 'string_list' || property.type === 'int_list'}
			<div class="flex flex-col gap-1">
				<span id="{inputId}-label">{name}</span>
				{#if description !== descriptionKey}
					<span class="text-hint">{description}</span>
				{/if}

				{#if property.type === 'list_multiple'}
					{@const listTypeId = property.listType ?? ''}
					{@const selectedKeys = asStringArray(getValue(id))}
					{@const elements = listTypeId ? selectableListElements(listCache[listTypeId] ?? [], selectedKeys) : []}
					<div
						id={inputId}
						class="flex flex-col gap-2"
						role="group"
						aria-labelledby="{inputId}-label"
						aria-describedby={describedBy}
					>
						{#if listTypeId && listLoading[listTypeId] && elements.length === 0}
							<span class="text-hint">{t('web.common.loading')}</span>
						{:else}
							<TransferList
								items={elements}
								bind:selected={() => selectedKeys, v => setValue(id, v)}
								getKey={element => element.type}
								getSearchText={element => `${listElementName(element.type)} ${element.type}`}
								disabled={submitting}
								labelledBy="{inputId}-label"
								compareAvailable={(a, b) => a.index - b.index}
							>
								{#snippet item(element)}
									<span class="font-medium">{listElementName(element.type)}</span>
								{/snippet}
							</TransferList>
						{/if}
					</div>
				{:else if property.type === 'string_list'}
					<ConfigStringListInput
						id={inputId}
						bind:value={() => asStringArray(getValue(id)), v => setValue(id, v)}
						disabled={submitting}
					/>
				{:else}
					<ConfigIntListInput
						id={inputId}
						bind:value={() => asNumberArray(getValue(id)), v => setValue(id, v)}
						disabled={submitting}
					/>
				{/if}

				{#if fieldError}
					<span id={errorId} class="text-sm text-destructive" role="alert">
						{tApiErrorResponse(fieldError)}
					</span>
				{/if}
			</div>
		{:else}
			<label class="flex flex-col gap-1">
				<span>{name}</span>
				{#if description !== descriptionKey}
					<span class="text-hint">{description}</span>
				{/if}

				{#if property.type === 'string' || property.type === 'uuid'}
					<input
						id={inputId}
						type="text"
						name={id}
						class="input w-full"
						bind:value={() => asString(getValue(id)), v => setValue(id, v)}
						disabled={submitting}
						aria-invalid={invalid}
						aria-describedby={describedBy}
						placeholder=" "
					/>
				{:else if property.type === 'number' || property.type === 'decimal'}
					<input
						id={inputId}
						type="number"
						name={id}
						step={property.type === 'decimal' ? 'any' : '1'}
						class="input w-full"
						value={getValue(id) == null ? '' : String(getValue(id))}
						disabled={submitting}
						aria-invalid={invalid}
						aria-describedby={describedBy}
						placeholder=" "
						oninput={event => onNumberInput(id, event.currentTarget.value)}
					/>
				{:else if property.type === 'date'}
					<input
						id={inputId}
						type="datetime-local"
						name={id}
						class="input w-full"
						value={toDatetimeLocal(getValue(id))}
						disabled={submitting}
						aria-invalid={invalid}
						aria-describedby={describedBy}
						oninput={event => setValue(id, fromDatetimeLocal(event.currentTarget.value))}
					/>
				{:else if property.type === 'list_item'}
					{@const listTypeId = property.listType ?? ''}
					{@const selectedKey = asString(getValue(id))}
					{@const elements = listTypeId
						? selectableListElements(listCache[listTypeId] ?? [], selectedKey ? [selectedKey] : [])
						: []}
					<select
						id={inputId}
						name={id}
						class="input w-full"
						bind:value={() => selectedKey, v => setValue(id, v || null)}
						disabled={submitting || Boolean(listTypeId && listLoading[listTypeId])}
						aria-invalid={invalid}
						aria-describedby={describedBy}
					>
						<option value="">{t('web.common.select')}</option>
						{#each elements as element (element.type)}
							<option value={element.type}>{listElementName(element.type)}</option>
						{/each}
					</select>
				{:else if property.type === 'object'}
					<textarea
						id={inputId}
						name={id}
						class="input w-full min-h-28 font-mono text-sm"
						value={getJsonDraft(id)}
						disabled={submitting}
						aria-invalid={invalid}
						aria-describedby={describedBy}
						placeholder={'{}'}
						oninput={event => onJsonInput(id, event.currentTarget.value)}></textarea>
				{:else}
					<input
						id={inputId}
						type="text"
						name={id}
						class="input w-full"
						bind:value={() => asString(getValue(id)), v => setValue(id, v)}
						disabled={submitting}
						aria-invalid={invalid}
						aria-describedby={describedBy}
						placeholder=" "
					/>
				{/if}

				{#if fieldError}
					<span id={errorId} class="text-sm text-destructive" role="alert">
						{tApiErrorResponse(fieldError)}
					</span>
				{/if}
			</label>
		{/if}
	{/each}

	{@render after?.()}
</div>
