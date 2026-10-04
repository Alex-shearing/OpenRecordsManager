<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { RecordController } from '$lib/api';
	import type { ApiFieldError, RecordResponse, RecordTypeResponse } from '$lib/api/types.gen';
	import { auditHeaders, getApiClient } from '$lib/api-client';
	import AuditSaveCard from '$lib/components/AuditSaveCard.svelte';
	import ObjectPropertyForm from '$lib/components/ObjectPropertyForm.svelte';
	import type { SchemaFormError } from '$lib/components/SchemaForm.svelte';
	import PageContent from '$lib/components/layout/PageContent.svelte';
	import { t, tApiErrorResponse } from '$lib/i18n/catalog';
	import { recordTypeName } from '$lib/i18n/labels';
	import {
		compactPropertyValues,
		filterCreateFields,
		seedCreateValues,
		toTypePropertyAssignments,
		valuesFromExistingProperties,
	} from '$lib/properties/createFields';
	import toast from 'svelte-hot-french-toast';

	let {
		mode,
		types,
		auditRequired,
		record,
		loadError,
	}: {
		mode: 'create' | 'edit';
		types: RecordTypeResponse[];
		auditRequired: boolean;
		record?: RecordResponse;
		loadError?: ApiFieldError;
	} = $props();

	const formId = 'record-editor-form';
	const isEdit = $derived(mode === 'edit');

	const sortedTypes = $derived([...types].toSorted((a, b) => recordTypeName(a.id).localeCompare(recordTypeName(b.id))));

	function fieldsForType(typeId: string) {
		const type = types.find(entry => entry.id === typeId);
		return type ? filterCreateFields(toTypePropertyAssignments(type.properties), { omitRecordIdentity: true }) : [];
	}

	function initialTypeId() {
		if (mode === 'edit' && record) {
			return record.type;
		}
		return types.at(0)?.id ?? '';
	}

	function initialTitle() {
		if (mode === 'edit' && record) {
			const titleValue = record.properties['builtin:title'];
			return typeof titleValue === 'string' ? titleValue : '';
		}
		return '';
	}

	function initialValues(typeId: string) {
		const fields = fieldsForType(typeId);
		if (mode === 'edit' && record) {
			return valuesFromExistingProperties(fields, record.properties);
		}
		return seedCreateValues(fields);
	}

	let typeId = $state(initialTypeId());
	let title = $state(initialTitle());
	let values = $state(initialValues(initialTypeId()));
	let formError = $state<SchemaFormError>();
	let auditComment = $state('');
	let submitting = $state(false);

	function snapshot() {
		return { typeId, title, values: $state.snapshot(values) };
	}

	let baseline = $state(snapshot());

	const fields = $derived(fieldsForType(typeId));

	function reset() {
		if (isEdit && record) {
			void goto(resolve('/(authenticated)/records/[id]', { id: record.id }));
			return;
		}

		typeId = baseline.typeId;
		title = baseline.title;
		values = $state.snapshot(baseline.values);
		formError = undefined;
	}

	async function handleSubmit(event: SubmitEvent) {
		event.preventDefault();

		if (mode === 'create') {
			submitting = true;
			formError = undefined;

			const { data, error } = await RecordController.newRecord({
				client: getApiClient(),
				body: { type: typeId, title, properties: compactPropertyValues(values) },
				headers: auditHeaders(auditComment),
			});

			submitting = false;

			if (error) {
				formError = error;
				return;
			}

			toast.success(t('web.records.created'));

			await goto(resolve('/(authenticated)/records/[id]', { id: data.data.id }));

			return;
		}

		if (!record) {
			return;
		}

		submitting = true;
		formError = undefined;

		const { error } = await RecordController.update1({
			client: getApiClient(),
			path: { id: record.id },
			body: { title, properties: compactPropertyValues(values) },
			headers: auditHeaders(auditComment),
		});

		submitting = false;

		if (error) {
			formError = error;
			return;
		}

		toast.success(t('web.records.updated'));
		await goto(resolve('/(authenticated)/records/[id]', { id: record.id }));
	}
</script>

<PageContent>
	{#if isEdit}
		<h1 class="mb-6 text-2xl font-semibold">{t('web.records.edit_title')}</h1>
	{:else}
		<h1 class="mb-6 text-2xl font-semibold">{t('web.records.create_title')}</h1>
	{/if}

	{#if loadError}
		<p class="text-destructive">{tApiErrorResponse(loadError)}</p>
	{:else if isEdit && !record}
		<p class="text-destructive">{t('web.records.not_found')}</p>
	{:else if !isEdit && sortedTypes.length === 0}
		<p class="text-hint">{t('web.records.no_types')}</p>
	{:else}
		<form id={formId} class="card p-5" onsubmit={handleSubmit}>
			{#if isEdit}
				<div class="mb-4 flex flex-col gap-1">
					<span class="text-label">{t('web.common.type')}</span>
					<p class="text-sm">{recordTypeName(typeId)}</p>
				</div>
			{:else}
				<label class="mb-4 flex flex-col gap-1">
					<span class="text-label">{t('web.common.type')}</span>
					<select
						class="input w-full"
						bind:value={typeId}
						disabled={submitting}
						onchange={() => {
							values = seedCreateValues(fieldsForType(typeId));
							title = '';
							formError = undefined;
						}}
					>
						{#each sortedTypes as type (type.id)}
							<option value={type.id}>{recordTypeName(type.id)}</option>
						{/each}
					</select>
				</label>
			{/if}

			<label class="mb-4 flex flex-col gap-1">
				<span class="text-label">{t('web.records.title')}</span>
				<input class="input w-full" bind:value={title} required disabled={submitting} />
			</label>

			{#key typeId}
				<ObjectPropertyForm {fields} bind:values error={formError} {submitting} />
			{/key}
		</form>

		<AuditSaveCard
			form={formId}
			bind:auditComment
			required={auditRequired}
			error={formError}
			{submitting}
			dirty={true}
			onreset={reset}
		/>
	{/if}
</PageContent>
