<script lang="ts">
	import { PluginController } from '$lib/api';
	import type { SimplePluginResponse } from '$lib/api/types.gen';
	import { auditHeaders, getApiClient } from '$lib/api-client';
	import AuditSaveCard from '$lib/components/AuditSaveCard.svelte';
	import DialogActions from '$lib/components/DialogActions.svelte';
	import MonoId from '$lib/components/MonoId.svelte';
	import TableCard from '$lib/components/TableCard.svelte';
	import TargetDialog from '$lib/components/TargetDialog.svelte';
	import { invalidateAll } from '$app/navigation';
	import toast from 'svelte-hot-french-toast';

	let { data } = $props();

	const sortedPlugins = $derived(
		[...data.plugins].sort((a, b) => {
			const byDisplay = (a.displayName ?? a.id ?? '').localeCompare(b.displayName ?? b.id ?? '');
			if (byDisplay !== 0) {
				return byDisplay;
			}
			return (a.id ?? '').localeCompare(b.id ?? '');
		})
	);

	function enabledDraft(plugins: SimplePluginResponse[]) {
		return Object.fromEntries(
			plugins
				.filter(plugin => plugin.id && plugin.id !== 'builtin')
				.map(plugin => [plugin.id!, plugin.enabled ?? false])
		);
	}

	// svelte-ignore state_referenced_locally
	let draftEnabled = $state(enabledDraft(data.plugins));
	let uploadFiles = $state<FileList>();
	let auditComment = $state('');
	let submitting = $state(false);
	let formError = $state('');
	let deleteTarget = $state<SimplePluginResponse>();

	const dirtyIds = $derived(
		sortedPlugins
			.filter(
				plugin => plugin.id && plugin.id !== 'builtin' && draftEnabled[plugin.id] !== (plugin.enabled ?? false)
			)
			.map(plugin => plugin.id!)
	);

	$effect(() => {
		for (const plugin of data.plugins) {
			if (!plugin.id || plugin.id === 'builtin' || plugin.id in draftEnabled) {
				continue;
			}
			draftEnabled[plugin.id] = plugin.enabled ?? false;
		}
	});

	function resetDraft() {
		draftEnabled = enabledDraft(data.plugins);
		formError = '';
	}

	async function handleUpload(event: SubmitEvent) {
		event.preventDefault();

		const file = uploadFiles?.[0];
		if (!file) {
			formError = 'Select a plugin JAR or ZIP file to upload.';
			return;
		}

		if (data.auditCommentRequired.create && !auditComment.trim()) {
			formError = 'An audit comment is required for this action.';
			return;
		}

		submitting = true;
		formError = '';

		const type = file.name.toLowerCase().endsWith('.zip') ? 'zip' : 'jar';
		const { error } = await PluginController.uploadPlugin({
			client: getApiClient(),
			body: { file },
			query: { type },
			headers: auditHeaders(auditComment),
		});

		submitting = false;

		if (error) {
			formError = error.error ?? 'Failed to upload plugin.';
			return;
		}

		toast.success(`Uploaded ${file.name}.`);
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
		formError = '';

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
					formError = `Failed to update plugin ${id}: ${error.error}`;
					return;
				}
			}

			toast.success(dirtyIds.length === 1 ? 'Saved plugin changes.' : `Saved changes to ${dirtyIds.length} plugins.`);
			await invalidateAll();
		} finally {
			submitting = false;
		}
	}

	async function confirmDelete() {
		if (!deleteTarget?.id || deleteTarget.id === 'builtin') {
			return;
		}

		if (data.auditCommentRequired.delete && !auditComment.trim()) {
			formError = 'An audit comment is required for this action.';
			return;
		}

		submitting = true;
		formError = '';

		const id = deleteTarget.id;
		const { error } = await PluginController.deletePlugin({
			client: getApiClient(),
			path: { id },
			headers: auditHeaders(auditComment),
		});

		submitting = false;
		deleteTarget = undefined;

		if (error) {
			formError = 'Failed to delete plugin: ' + error.error;
			return;
		}

		toast.success(`Deleted plugin ${id}.`);
		auditComment = '';
		await invalidateAll();
	}
</script>

<h1 class="mb-2 text-2xl font-semibold">Plugins</h1>
<p class="mb-6 text-hint">
	Upload plugin JARs or template ZIPs, enable or disable plugins, and remove plugins from the workgroup.
</p>

{#if data.error}
	<p class="text-destructive">{data.error}</p>
{:else}
	<form id="plugins-save-form" onsubmit={handleSave}>
		<TableCard title="Installed plugins" items={sortedPlugins} empty="No plugins are registered." getKey={p => p.id}>
			{#snippet header()}
				<th class="px-5 py-3 font-medium">Plugin</th>
				<th class="px-5 py-3 font-medium">Enabled</th>
				<th class="px-5 py-3 font-medium">Modified</th>
				<th class="px-5 py-3 font-medium"><span class="sr-only">Actions</span></th>
			{/snippet}
			{#snippet row(plugin)}
				<td class="px-5 py-4">
					<p class="font-medium">{plugin.displayName ?? plugin.id}</p>
					<p class="text-hint"><MonoId value={plugin.id!} /></p>
					{#if plugin.description}
						<p class="text-hint">{plugin.description}</p>
					{/if}
					<p class="text-hint">Version {plugin.version ?? '—'}</p>
					{#if !plugin.loaded}
						<p class="text-hint">Not loaded</p>
					{/if}
				</td>
				<td class="px-5 py-4">
					{#if plugin.id !== 'builtin'}
						<input
							type="checkbox"
							class="size-4 rounded border-border-input"
							disabled={submitting}
							aria-label="Enable {plugin.displayName ?? plugin.id}"
							bind:checked={draftEnabled[plugin.id!]}
						/>
					{:else}
						<span class="text-hint">Always on</span>
					{/if}
				</td>
				<td class="px-5 py-4 text-foreground">
					<time datetime={plugin.dateModified}>
						{new Date(plugin.dateModified).toLocaleString()}
					</time>
				</td>
				<td class="px-5 py-4 text-right">
					{#if plugin.id !== 'builtin'}
						<button
							type="button"
							class="btn-ghost text-destructive"
							disabled={submitting}
							onclick={() => (deleteTarget = plugin)}
						>
							Delete
						</button>
					{/if}
				</td>
			{/snippet}
		</TableCard>
	</form>

	<form class="mt-6 card p-5" onsubmit={handleUpload}>
		<h2 class="mb-1 text-lg font-medium">Upload plugin</h2>
		<p class="mb-4 text-hint">
			Select a JAR or ZIP with a root plugin.json (id, version, displayName, and description). ZIPs are
			templates-only.
		</p>

		<label class="flex flex-col gap-1">
			<span class="text-label">Plugin archive</span>
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
				{submitting ? 'Uploading…' : 'Upload'}
			</button>
		</div>
	</form>

	<AuditSaveCard
		form="plugins-save-form"
		bind:auditComment
		required={data.auditCommentRequired.update}
		requiredHint="Required when saving plugin changes."
		{formError}
		{submitting}
		dirty={dirtyIds.length > 0}
		onreset={resetDraft}
	/>
{/if}

<TargetDialog bind:target={deleteTarget} title="Delete plugin">
	{#snippet description(target)}
		Remove <MonoId value={target.id} /> from the database and this server?
	{/snippet}
	{#snippet footer()}
		<DialogActions
			variant="destructive"
			confirmLabel="Delete"
			confirmingLabel="Deleting…"
			{submitting}
			onconfirm={confirmDelete}
		/>
	{/snippet}
</TargetDialog>
