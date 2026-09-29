<script lang="ts">
	import { PluginController } from '$lib/api';
	import type { SimplePluginResponse } from '$lib/api/types.gen';
	import { auditHeaders, getApiClient } from '$lib/api-client';
	import AuditSaveCard from '$lib/components/AuditSaveCard.svelte';
	import DialogActions from '$lib/components/DialogActions.svelte';
	import MonoId from '$lib/components/MonoId.svelte';
	import TableCard from '$lib/components/TableCard.svelte';
	import TargetDialog from '$lib/components/TargetDialog.svelte';
	import { type SchemaFormError } from '$lib/components/SchemaForm.svelte';
	import { t, tApiErrorResponse } from '$lib/i18n/catalog';
	import { pluginDescription, pluginName } from '$lib/i18n/labels';
	import { invalidateAll } from '$app/navigation';
	import toast from 'svelte-hot-french-toast';

	let { data } = $props();

	const sortedPlugins = $derived(
		[...data.plugins].sort((a, b) => {
			const byDisplay = pluginName(a.id).localeCompare(pluginName(b.id));
			if (byDisplay !== 0) {
				return byDisplay;
			}
			return a.id.localeCompare(b.id);
		})
	);

	function enabledDraft(plugins: SimplePluginResponse[]) {
		return Object.fromEntries(plugins.filter(plugin => plugin.id).map(plugin => [plugin.id!, plugin.enabled ?? false]));
	}

	// svelte-ignore state_referenced_locally
	let draftEnabled = $state(enabledDraft(data.plugins));
	let uploadFiles = $state<FileList>();
	let auditComment = $state('');
	let submitting = $state(false);
	let formError = $state<SchemaFormError>();
	let deleteTarget = $state<SimplePluginResponse>();

	const dirtyIds = $derived(
		sortedPlugins
			.filter(plugin => plugin.id && draftEnabled[plugin.id] !== (plugin.enabled ?? false))
			.map(plugin => plugin.id!)
	);

	$effect(() => {
		for (const plugin of data.plugins) {
			if (!plugin.id || plugin.id in draftEnabled) {
				continue;
			}
			draftEnabled[plugin.id] = plugin.enabled ?? false;
		}
	});

	function resetDraft() {
		draftEnabled = enabledDraft(data.plugins);
		formError = undefined;
	}

	async function handleUpload(event: SubmitEvent) {
		event.preventDefault();

		const file = uploadFiles?.[0];
		if (!file) {
			formError = { error: 'web.plugins.select_file' };
			return;
		}

		if (data.auditCommentRequired.create && !auditComment.trim()) {
			formError = { error: 'audit_comment_required' };
			return;
		}

		submitting = true;
		formError = undefined;

		const type = file.name.toLowerCase().endsWith('.zip') ? 'zip' : 'jar';
		const { error } = await PluginController.uploadPlugin({
			client: getApiClient(),
			body: { file },
			query: { type },
			headers: auditHeaders(auditComment),
		});

		submitting = false;

		if (error) {
			formError = error;
			return;
		}

		toast.success(t('web.plugins.uploaded', file.name));
		uploadFiles = undefined;
		auditComment = '';
		await invalidateAll();
	}

	async function handleSave(event: SubmitEvent) {
		event.preventDefault();

		if (dirtyIds.length === 0) {
			return;
		}

		submitting = true;
		formError = undefined;

		try {
			const headers = auditHeaders(auditComment);
			const client = getApiClient();

			for (const id of dirtyIds) {
				const { error } = await PluginController.updatePlugin({
					client,
					path: { id },
					body: { enabled: draftEnabled[id] },
					headers,
				});

				if (error) {
					formError = error;
					return;
				}
			}

			toast.success(dirtyIds.length === 1 ? t('web.plugins.saved_one') : t('web.plugins.saved_many', dirtyIds.length));
			await invalidateAll();
		} finally {
			submitting = false;
		}
	}

	async function confirmDelete() {
		if (!deleteTarget?.id) {
			return;
		}

		if (data.auditCommentRequired.delete && !auditComment.trim()) {
			formError = { error: 'audit_comment_required' };
			return;
		}

		submitting = true;
		formError = undefined;

		const id = deleteTarget.id;
		const { error } = await PluginController.deletePlugin({
			client: getApiClient(),
			path: { id },
			headers: auditHeaders(auditComment),
		});

		submitting = false;
		deleteTarget = undefined;

		if (error) {
			formError = error;
			return;
		}

		toast.success(t('web.plugins.deleted', id));
		auditComment = '';
		await invalidateAll();
	}
</script>

<h1 class="mb-2 text-2xl font-semibold">{t('web.plugins.title')}</h1>
<p class="mb-6 text-hint">{t('web.plugins.intro')}</p>

{#if data.error}
	<p class="text-destructive">{tApiErrorResponse(data.error)}</p>
{:else}
	<form id="plugins-save-form" onsubmit={handleSave}>
		<TableCard
			title={t('web.plugins.installed')}
			items={sortedPlugins}
			empty={t('web.plugins.empty')}
			getKey={p => p.id}
		>
			{#snippet header()}
				<th class="px-5 py-3 font-medium">{t('web.plugins.col_plugin')}</th>
				<th class="px-5 py-3 font-medium">{t('web.plugins.col_enabled')}</th>
				<th class="px-5 py-3 font-medium">{t('web.plugins.col_modified')}</th>
				<th class="px-5 py-3 font-medium"><span class="sr-only">{t('web.common.actions')}</span></th>
			{/snippet}
			{#snippet row(plugin)}
				<td class="px-5 py-4">
					<p class="font-medium">{pluginName(plugin.id)}</p>
					<p class="text-hint"><MonoId value={plugin.id} /></p>
					<p class="text-hint">{pluginDescription(plugin.id)}</p>
					<p class="text-hint">{t('web.common.version', plugin.version ?? t('web.common.em_dash'))}</p>
					{#if !plugin.loaded}
						<p class="text-hint">{t('web.plugins.not_loaded')}</p>
					{/if}
				</td>
				<td class="px-5 py-4">
					<input
						type="checkbox"
						class="size-4 rounded border-border-input"
						disabled={submitting}
						aria-label={t('web.plugins.enable_aria', pluginName(plugin.id))}
						bind:checked={draftEnabled[plugin.id]}
					/>
				</td>
				<td class="px-5 py-4 text-foreground">
					<time datetime={plugin.dateModified}>
						{new Date(plugin.dateModified).toLocaleString()}
					</time>
				</td>
				<td class="px-5 py-4 text-right">
					<button
						type="button"
						class="btn-ghost text-destructive"
						disabled={submitting}
						onclick={() => (deleteTarget = plugin)}
					>
						{t('web.common.delete')}
					</button>
				</td>
			{/snippet}
		</TableCard>
	</form>

	<form class="mt-6 card p-5" onsubmit={handleUpload}>
		<h2 class="mb-1 text-lg font-medium">{t('web.plugins.upload_title')}</h2>
		<p class="mb-4 text-hint">{t('web.plugins.upload_hint')}</p>

		<label class="flex flex-col gap-1">
			<span class="text-label">{t('web.plugins.archive_label')}</span>
			<input
				type="file"
				name="file"
				accept=".jar,.zip,application/java-archive,application/zip"
				disabled={submitting}
				class="input w-full max-w-md"
				bind:files={uploadFiles}
			/>
		</label>

		<div class="mt-4">
			<button type="submit" class="btn-primary" disabled={submitting || !uploadFiles?.length}>
				{submitting ? t('web.plugins.uploading') : t('web.plugins.upload')}
			</button>
		</div>
	</form>

	<AuditSaveCard
		form="plugins-save-form"
		bind:auditComment
		required={data.auditCommentRequired.update}
		error={formError}
		{submitting}
		dirty={dirtyIds.length > 0}
		onreset={resetDraft}
	/>
{/if}

<TargetDialog bind:target={deleteTarget} title="web.plugins.delete_title">
	{#snippet description(target)}
		{t('web.plugins.remove_confirm', target.id ?? '')}
	{/snippet}
	{#snippet footer()}
		<DialogActions
			variant="destructive"
			confirmLabel="web.common.delete"
			confirmingLabel="web.common.deleting"
			{submitting}
			onconfirm={confirmDelete}
		/>
	{/snippet}
</TargetDialog>
