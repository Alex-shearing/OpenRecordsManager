<script lang="ts">
	import { ListController, type ListElementResponse, type ListTypeResponse } from '#lib/api/index.js';
	import { auditHeaders, getApiClient } from '#lib/api-client.js';
	import AdminCreateForm from '#lib/components/AdminCreateForm.svelte';
	import AdminFormError from '#lib/components/AdminFormError.svelte';
	import AdminPageIntro from '#lib/components/AdminPageIntro.svelte';
	import AuditCommentField from '#lib/components/AuditCommentField.svelte';
	import DialogActions from '#lib/components/DialogActions.svelte';
	import ListElementFields from '#lib/components/ListElementFields.svelte';
	import MonoId from '#lib/components/MonoId.svelte';
	import TableCard from '#lib/components/TableCard.svelte';
	import TargetDialog from '#lib/components/TargetDialog.svelte';
	import type { SchemaFormError } from '#lib/components/SchemaForm.svelte';
	import { t, tApiErrorResponse } from '#lib/i18n/catalog.js';
	import { listElementName, listTypeName } from '#lib/i18n/labels.js';
	import { isBuiltinResourceId } from '#lib/types/typeAdmin.js';
	import { refreshAll } from '$app/navigation';
	import toast from 'svelte-hot-french-toast';

	let { data } = $props();

	const listItems = $derived(data.listIds.map(id => ({ id })));

	let createId = $state('');
	let createName = $state('');
	let createAuditComment = $state('');
	let createError = $state<SchemaFormError>();
	let submitting = $state(false);

	let editTarget = $state<ListTypeResponse>();
	let editListId = $state('');
	let editName = $state('');
	let editAuditComment = $state('');
	let editError = $state<SchemaFormError>();
	let editLoading = $state(false);

	let elementId = $state('');
	let elementName = $state('');
	let elementDescription = $state('');
	let elementIndex = $state(0);
	let elementAliases = $state('');
	let elementAuditComment = $state('');
	let elementError = $state<SchemaFormError>();

	let editElement = $state<ListElementResponse>();
	let editElementName = $state('');
	let editElementDescription = $state('');
	let editElementIndex = $state(0);
	let editElementAliases = $state('');
	let editElementAuditComment = $state('');
	let editElementError = $state<SchemaFormError>();

	function parseAliases(raw: string): string[] {
		return raw
			.split(',')
			.map(entry => entry.trim())
			.filter(Boolean);
	}

	async function handleCreate(event: SubmitEvent) {
		event.preventDefault();
		submitting = true;
		createError = undefined;

		const { error } = await ListController.createList({
			client: getApiClient(),
			body: { id: createId.trim(), name: createName.trim() },
			headers: auditHeaders(createAuditComment),
		});

		submitting = false;
		if (error) {
			createError = error;
			return;
		}

		toast.success(t('web.lists.created'));
		createId = '';
		createName = '';
		createAuditComment = '';
		await refreshAll();
	}

	async function openEdit(listId: string) {
		editLoading = true;
		editError = undefined;
		editAuditComment = '';
		elementError = undefined;
		editElement = undefined;

		const { data: result, error } = await ListController.getList({
			client: getApiClient(),
			path: { list: listId },
		});
		editLoading = false;

		if (error) {
			toast.error(tApiErrorResponse(error));
			return;
		}

		editListId = listId;
		editTarget = result.data;
		editName = listTypeName(listId);
		elementIndex = result.data.elements.length;
	}

	async function handleEditList(event: SubmitEvent) {
		event.preventDefault();
		if (!editListId) return;
		submitting = true;
		editError = undefined;

		const { error } = await ListController.updateList({
			client: getApiClient(),
			path: { list: editListId },
			body: { name: editName.trim() },
			headers: auditHeaders(editAuditComment),
		});

		submitting = false;
		if (error) {
			editError = error;
			return;
		}

		toast.success(t('web.lists.updated'));
		await openEdit(editListId);
		await refreshAll();
	}

	async function handleCreateElement(event: SubmitEvent) {
		event.preventDefault();
		if (!editListId) return;
		submitting = true;
		elementError = undefined;

		const { error } = await ListController.createListElement({
			client: getApiClient(),
			path: { list: editListId },
			body: {
				id: elementId.trim(),
				name: elementName.trim(),
				description: elementDescription.trim(),
				index: elementIndex,
				aliases: parseAliases(elementAliases),
			},
			headers: auditHeaders(elementAuditComment),
		});

		submitting = false;
		if (error) {
			elementError = error;
			return;
		}

		toast.success(t('web.lists.element_created'));
		elementId = '';
		elementName = '';
		elementDescription = '';
		elementAliases = '';
		elementAuditComment = '';
		await openEdit(editListId);
		await refreshAll();
	}

	function openEditElement(element: ListElementResponse) {
		editElement = element;
		editElementName = listElementName(element.type);
		editElementDescription = '';
		editElementIndex = element.index;
		editElementAliases = element.aliases.join(', ');
		editElementAuditComment = '';
		editElementError = undefined;
	}

	async function handleEditElement(event: SubmitEvent) {
		event.preventDefault();
		if (!editListId || !editElement) return;
		submitting = true;
		editElementError = undefined;

		const { error } = await ListController.updateListElement({
			client: getApiClient(),
			path: { list: editListId, element: editElement.type },
			body: {
				name: editElementName.trim(),
				description: editElementDescription.trim(),
				index: editElementIndex,
				aliases: parseAliases(editElementAliases),
			},
			headers: auditHeaders(editElementAuditComment),
		});

		submitting = false;
		if (error) {
			editElementError = error;
			return;
		}

		toast.success(t('web.lists.element_updated'));
		editElement = undefined;
		await openEdit(editListId);
		await refreshAll();
	}
</script>

<AdminPageIntro title={t('web.lists.title')} intro={t('web.lists.intro')} error={data.error} />

{#if !data.error}
	<TableCard title={t('web.lists.table_title')} items={listItems} empty={t('web.lists.empty')} getKey={item => item.id}>
		{#snippet header()}
			<th class="px-5 py-3 font-medium">{t('web.common.id')}</th>
			<th class="px-5 py-3 font-medium">{t('web.common.name')}</th>
			<th class="px-5 py-3 font-medium"><span class="sr-only">{t('web.common.actions')}</span></th>
		{/snippet}
		{#snippet row(item)}
			<td class="px-5 py-4"><MonoId value={item.id} /></td>
			<td class="px-5 py-4">{listTypeName(item.id)}</td>
			<td class="px-5 py-4 text-right">
				<button type="button" class="btn-ghost" disabled={submitting || editLoading} onclick={() => openEdit(item.id)}>
					{t('web.common.edit')}
				</button>
			</td>
		{/snippet}
	</TableCard>

	<AdminCreateForm
		title={t('web.lists.create_title')}
		hint={t('web.lists.create_hint')}
		onsubmit={handleCreate}
		{submitting}
		submitDisabled={!createId.trim() || !createName.trim()}
		error={createError}
		bind:auditComment={createAuditComment}
		auditRequired={data.auditCommentRequired.create}
	>
		<label class="mb-4 flex flex-col gap-1">
			<span class="text-label">{t('web.common.id')}</span>
			<input
				class="input w-full font-mono"
				name="id"
				bind:value={createId}
				placeholder="custom:status"
				required
				disabled={submitting}
			/>
		</label>
		<label class="mb-4 flex flex-col gap-1">
			<span class="text-label">{t('web.common.name')}</span>
			<input class="input w-full" name="name" bind:value={createName} required disabled={submitting} />
		</label>
	</AdminCreateForm>
{/if}

<TargetDialog
	bind:target={editTarget}
	title="web.lists.edit_title"
	size="wide"
	onclose={() => {
		editListId = '';
		editElement = undefined;
	}}
>
	{#snippet description(_target)}
		{t('web.lists.edit_description', editListId)}
	{/snippet}
	{#snippet body(target)}
		{@const sortedElements = [...target.elements].sort((a, b) => a.index - b.index)}

		<form id="list-edit-form" class="flex flex-col gap-4" onsubmit={handleEditList}>
			<label class="flex flex-col gap-1">
				<span class="text-label">{t('web.common.name')}</span>
				<input
					class="input w-full"
					name="name"
					bind:value={editName}
					required
					disabled={submitting || isBuiltinResourceId(editListId)}
				/>
			</label>
			<AdminFormError error={editError} />
			<AuditCommentField
				bind:value={editAuditComment}
				required={data.auditCommentRequired.update}
				disabled={submitting}
				rows={2}
			/>
		</form>

		<section class="mt-6 border-t border-border pt-4" aria-labelledby="list-elements-heading">
			<h3 id="list-elements-heading" class="mb-3 text-base font-medium">{t('web.lists.elements_title')}</h3>
			{#if sortedElements.length === 0}
				<p class="mb-4 text-hint">{t('web.lists.elements_empty')}</p>
			{:else}
				<ul class="mb-4 divide-y divide-border rounded-md border border-border">
					{#each sortedElements as element (element.type)}
						<li class="flex items-center justify-between gap-3 px-3 py-2">
							<div class="min-w-0">
								<p class="font-medium">{listElementName(element.type)}</p>
								<p class="text-hint font-mono text-xs">{element.type} · #{element.index}</p>
							</div>
							<button
								type="button"
								class="btn-ghost shrink-0"
								disabled={submitting}
								onclick={() => openEditElement(element)}
							>
								{t('web.common.edit')}
							</button>
						</li>
					{/each}
				</ul>
			{/if}

			<form class="rounded-md border border-border p-3" onsubmit={handleCreateElement}>
				<fieldset class="flex flex-col gap-3" disabled={submitting}>
					<legend class="text-sm font-medium">{t('web.lists.element_create_title')}</legend>
					<ListElementFields
						showId
						bind:id={elementId}
						bind:name={elementName}
						bind:description={elementDescription}
						bind:index={elementIndex}
						bind:aliases={elementAliases}
						bind:auditComment={elementAuditComment}
						error={elementError}
						auditRequired={data.auditCommentRequired.createElement}
					/>
					<button type="submit" class="btn-secondary self-start" disabled={!elementId.trim() || !elementName.trim()}>
						{t('web.lists.element_create')}
					</button>
				</fieldset>
			</form>
		</section>
	{/snippet}
	{#snippet footer(_target)}
		<DialogActions
			formId="list-edit-form"
			confirmLabel="web.common.save"
			confirmingLabel="web.common.saving"
			{submitting}
			disabled={isBuiltinResourceId(editListId)}
		/>
	{/snippet}
</TargetDialog>

<TargetDialog bind:target={editElement} title="web.lists.element_edit_title">
	{#snippet description(target)}
		{t('web.lists.element_edit_description', target.type)}
	{/snippet}
	{#snippet body(_target)}
		<form id="list-element-edit-form" class="flex flex-col gap-4" onsubmit={handleEditElement}>
			<ListElementFields
				bind:name={editElementName}
				bind:description={editElementDescription}
				bind:index={editElementIndex}
				bind:aliases={editElementAliases}
				bind:auditComment={editElementAuditComment}
				error={editElementError}
				auditRequired={data.auditCommentRequired.updateElement}
				disabled={submitting}
			/>
		</form>
	{/snippet}
	{#snippet footer(_target)}
		<DialogActions
			formId="list-element-edit-form"
			confirmLabel="web.common.save"
			confirmingLabel="web.common.saving"
			{submitting}
		/>
	{/snippet}
</TargetDialog>
