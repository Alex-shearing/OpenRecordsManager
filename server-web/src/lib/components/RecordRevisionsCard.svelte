<script lang="ts">
	import { invalidateAll, refreshAll } from '$app/navigation';
	import { RecordController } from '#lib/api/index.js';
	import type { RecordResponse, RecordTypeResponse } from '#lib/api/types.gen.js';
	import type { SchemaFormError } from '#lib/components/SchemaForm.svelte';
	import AppDialog from '#lib/components/AppDialog.svelte';
	import DialogActions from '#lib/components/DialogActions.svelte';
	import MonoId from '#lib/components/MonoId.svelte';
	import TableCard from '#lib/components/TableCard.svelte';
	import { auditHeaders, getApiClient } from '#lib/api-client.js';
	import { t, tApiErrorResponse } from '#lib/i18n/catalog.js';
	import toast from 'svelte-hot-french-toast';

	let {
		record,
		types,
		auditCommentRequired,
	}: {
		record: RecordResponse;
		types: RecordTypeResponse[];
		auditCommentRequired: boolean;
	} = $props();

	const VERSION_PATTERN = /^[0-9]+(\.[0-9]+)*$/;
	const uploadFormId = 'record-revision-upload-form';

	let uploadDialogOpen = $state(false);
	let version = $state('');
	let uploadFiles = $state<FileList>();
	let auditComment = $state('');
	let submitting = $state(false);
	let formError = $state<SchemaFormError>();
	let revisionDropDepth = $state(0);

	const revisionDropActive = $derived(revisionDropDepth > 0);

	const recordType = $derived(types.find(entry => entry.id === record.type));
	const typeSupportsFiles = $derived(!recordType || (recordType.contentTypes?.length ?? 0) > 0);
	const canUploadRevision = $derived(record.canAccessRevisions && typeSupportsFiles);

	function revisionDownloadUrl(recordId: string, revisionVersion: string) {
		const base = getApiClient().getConfig().baseUrl || '';
		return `${base}/api/records/${recordId}/${encodeURIComponent(revisionVersion)}`;
	}

	function suggestNextVersion(revisions: string[]) {
		if (revisions.length === 0) {
			return '1';
		}
		const last = revisions[revisions.length - 1]!;
		const parts = last.split('.');
		const lastPart = Number(parts[parts.length - 1]);
		if (Number.isInteger(lastPart)) {
			parts[parts.length - 1] = String(lastPart + 1);
			return parts.join('.');
		}
		return '1';
	}

	function openUploadDialog(file?: File) {
		version = suggestNextVersion(record.revisions);
		if (file) {
			const dt = new DataTransfer();
			dt.items.add(file);
			uploadFiles = dt.files;
		} else {
			uploadFiles = undefined;
		}
		auditComment = '';
		formError = undefined;
		uploadDialogOpen = true;
	}

	function resetUploadDialog() {
		version = '';
		uploadFiles = undefined;
		auditComment = '';
		formError = undefined;
	}

	function isFileDrag(event: DragEvent) {
		return event.dataTransfer?.types.includes('Files') ?? false;
	}

	function onRevisionDragEnter(event: DragEvent) {
		if (!isFileDrag(event)) {
			return;
		}
		event.preventDefault();
		if (!canUploadRevision) {
			return;
		}
		revisionDropDepth += 1;
	}

	function onRevisionDragOver(event: DragEvent) {
		if (!isFileDrag(event)) {
			return;
		}
		event.preventDefault();
		if (event.dataTransfer) {
			event.dataTransfer.dropEffect = canUploadRevision ? 'copy' : 'none';
		}
	}

	function onRevisionDragLeave(event: DragEvent) {
		if (!isFileDrag(event) || !canUploadRevision) {
			return;
		}
		revisionDropDepth = Math.max(0, revisionDropDepth - 1);
	}

	function onRevisionDrop(event: DragEvent) {
		if (!isFileDrag(event)) {
			return;
		}
		event.preventDefault();
		revisionDropDepth = 0;
		if (!canUploadRevision) {
			return;
		}
		const file = event.dataTransfer?.files?.[0];
		if (!file) {
			return;
		}
		openUploadDialog(file);
	}

	async function handleUpload(event: SubmitEvent) {
		event.preventDefault();

		const trimmedVersion = version.trim();
		if (!VERSION_PATTERN.test(trimmedVersion)) {
			formError = { error: 'web.records.revision_version_invalid' };
			return;
		}

		const file = uploadFiles?.[0];
		if (!file) {
			formError = { error: 'web.records.revision_select_file' };
			return;
		}

		if (auditCommentRequired && !auditComment.trim()) {
			formError = { error: 'audit_comment_required' };
			return;
		}

		submitting = true;
		formError = undefined;

		const { error } = await RecordController.createRecordRevision({
			client: getApiClient(),
			path: { id: record.id, version: trimmedVersion },
			body: { stream: file },
			headers: auditHeaders(auditComment),
		});

		submitting = false;

		if (error) {
			formError = error;
			return;
		}

		toast.success(t('web.records.revision_uploaded', trimmedVersion));
		uploadDialogOpen = false;
		resetUploadDialog();
		await refreshAll();
	}
</script>

<TableCard
	title={t('web.records.revisions')}
	items={record.revisions}
	empty={t('web.records.revisions_empty')}
	getKey={rev => rev}
	class={['relative mb-8 rounded-lg', revisionDropActive && 'ring-2 ring-primary/60']}
	ondragenter={onRevisionDragEnter}
	ondragover={onRevisionDragOver}
	ondragleave={onRevisionDragLeave}
	ondrop={onRevisionDrop}
>
	{#snippet actions()}
		<button
			type="button"
			class="btn-primary"
			disabled={!canUploadRevision}
			title={!record.canAccessRevisions
				? t('web.records.revision_no_access')
				: !typeSupportsFiles
					? t('web.records.revision_no_file_support')
					: undefined}
			onclick={() => openUploadDialog()}
		>
			{t('web.records.revision_upload')}
		</button>
	{/snippet}
	{#snippet header()}
		<th class="px-5 py-3 font-medium">{t('web.records.revision_version')}</th>
	{/snippet}
	{#snippet row(rev)}
		<td class="px-5 py-4">
			{#if record.canAccessRevisions}
				<a
					href={revisionDownloadUrl(record.id, rev)}
					class="text-primary underline-offset-2 hover:underline"
					data-sveltekit-reload
				>
					<MonoId value={rev} />
				</a>
			{:else}
				<MonoId value={rev} />
			{/if}
		</td>
	{/snippet}
	{#snippet overlay()}
		{#if revisionDropActive}
			<div
				class="pointer-events-none absolute inset-0 z-10 flex items-center justify-center rounded-lg bg-primary/10"
				aria-hidden="true"
			>
				<p class="rounded-md border border-border bg-surface px-3 py-2 text-sm font-medium shadow-sm">
					{t('web.records.revision_drop_hint')}
				</p>
			</div>
		{/if}
	{/snippet}
</TableCard>

<AppDialog bind:open={uploadDialogOpen} title="web.records.revision_upload_title" onclose={resetUploadDialog}>
	{#snippet body()}
		<form id={uploadFormId} class="flex flex-col gap-4" onsubmit={handleUpload}>
			<label class="flex flex-col gap-1">
				<span class="text-label">{t('web.records.revision_version')}</span>
				<input class="input w-full" bind:value={version} required disabled={submitting} />
			</label>

			<label class="flex flex-col gap-1">
				<span class="text-label">{t('web.records.revision_file')}</span>
				<input type="file" class="input w-full" disabled={submitting} bind:files={uploadFiles} />
			</label>

			<label class="flex flex-col gap-1">
				<span class="text-label">{t('web.common.audit_comment')}</span>
				<textarea
					class="input w-full"
					bind:value={auditComment}
					required={auditCommentRequired}
					disabled={submitting}
					rows={3}></textarea>
			</label>

			{#if formError}
				<p class="text-sm text-destructive" role="alert">{tApiErrorResponse(formError)}</p>
			{/if}
		</form>
	{/snippet}
	{#snippet footer()}
		<DialogActions
			formId={uploadFormId}
			confirmLabel="web.records.revision_upload"
			confirmingLabel="web.records.revision_uploading"
			{submitting}
			disabled={!uploadFiles?.length || !version.trim()}
		/>
	{/snippet}
</AppDialog>
