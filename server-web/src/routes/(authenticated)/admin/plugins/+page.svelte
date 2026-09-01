<script lang="ts">
	import { PluginController } from '$lib/api';
	import type { SimplePluginResponse } from '$lib/api/types.gen';
	import { getApiClient } from '$lib/api-client';
	import AuditSaveCard from '$lib/components/AuditSaveCard.svelte';
	import AppDialog from '$lib/components/AppDialog.svelte';
	import DialogActions from '$lib/components/DialogActions.svelte';
	import { invalidateAll } from '$app/navigation';

	let { data } = $props();

	const sortedPlugins = $derived(
		[...data.plugins].sort((a, b) => (a.name ?? '').localeCompare(b.name ?? ''))
	);

	function enabledDraft(plugins: SimplePluginResponse[]) {
		return Object.fromEntries(
			plugins
				.filter(plugin => plugin.name && plugin.name !== 'builtin')
				.map(plugin => [plugin.name!, plugin.enabled ?? false])
		);
	}

	// svelte-ignore state_referenced_locally
	let draftEnabled = $state(enabledDraft(data.plugins));
	let uploadFiles = $state<FileList>();
	let auditComment = $state('');
	let submitting = $state(false);
	let formError = $state('');
	let successMessage = $state('');
	let deleteTarget = $state<SimplePluginResponse | null>(null);
	let deleteOpen = $state(false);

	const auditRequired = $derived(
		data.auditCommentRequired.create ||
			data.auditCommentRequired.update ||
			data.auditCommentRequired.delete
	);

	const dirtyNames = $derived(
		sortedPlugins
			.filter(
				plugin =>
					plugin.name &&
					plugin.name !== 'builtin' &&
					draftEnabled[plugin.name] !== (plugin.enabled ?? false)
			)
			.map(plugin => plugin.name!)
	);

	$effect(() => {
		for (const plugin of data.plugins) {
			if (!plugin.name || plugin.name === 'builtin' || plugin.name in draftEnabled) {
				continue;
			}
			draftEnabled[plugin.name] = plugin.enabled ?? false;
		}
	});

	function resetDraft() {
		draftEnabled = enabledDraft(data.plugins);
		formError = '';
		successMessage = '';
	}

	async function handleUpload(event: SubmitEvent) {
		event.preventDefault();

		const file = uploadFiles?.[0];
		if (!file) {
			formError = 'Select a plugin JAR file to upload.';
			return;
		}

		if (data.auditCommentRequired.create && !auditComment.trim()) {
			formError = 'An audit comment is required for this action.';
			return;
		}

		submitting = true;
		formError = '';
		successMessage = '';

		const { error } = await PluginController.uploadPlugin({
			client: getApiClient(),
			body: { jar: file },
			headers: auditComment.trim() ? { 'X-ORM-Audit-Comment': auditComment.trim() } : undefined,
		});

		submitting = false;

		if (error) {
			formError = error.error ?? 'Failed to upload plugin.';
			return;
		}

		successMessage = `Uploaded ${file.name}.`;
		uploadFiles = undefined;
		auditComment = '';
		await invalidateAll();
	}

	async function handleSave(event: SubmitEvent) {
		event.preventDefault();

		if (dirtyNames.length === 0) {
			return;
		}

		if (data.auditCommentRequired.update && !auditComment.trim()) {
			formError = 'An audit comment is required for this action.';
			return;
		}

		submitting = true;
		formError = '';
		successMessage = '';

		const headers = auditComment.trim() ? { 'X-ORM-Audit-Comment': auditComment.trim() } : undefined;
		const client = getApiClient();

		for (const name of dirtyNames) {
			const { error } = await PluginController.updatePlugin({
				client,
				path: { name },
				body: { enabled: draftEnabled[name] },
				headers,
			});

			if (error) {
				submitting = false;
				formError = error.error ?? `Failed to update plugin ${name}.`;
				await invalidateAll();
				return;
			}
		}

		submitting = false;
		successMessage =
			dirtyNames.length === 1 ? 'Saved plugin changes.' : `Saved changes to ${dirtyNames.length} plugins.`;
		await invalidateAll();
	}

	async function confirmDelete() {
		if (!deleteTarget?.name || deleteTarget.name === 'builtin') {
			return;
		}

		if (data.auditCommentRequired.delete && !auditComment.trim()) {
			formError = 'An audit comment is required for this action.';
			return;
		}

		submitting = true;
		formError = '';
		successMessage = '';

		const name = deleteTarget.name;
		const { error } = await PluginController.deletePlugin({
			client: getApiClient(),
			path: { name },
			headers: auditComment.trim() ? { 'X-ORM-Audit-Comment': auditComment.trim() } : undefined,
		});

		submitting = false;
		deleteOpen = false;
		deleteTarget = null;

		if (error) {
			formError = error.error ?? 'Failed to delete plugin.';
			return;
		}

		successMessage = `Deleted plugin ${name}.`;
		auditComment = '';
		await invalidateAll();
	}
</script>

<h1 class="mb-2 text-2xl font-semibold">Plugins</h1>
<p class="mb-6 text-hint">
	Upload plugin JARs, enable or disable plugins, and remove plugins from the cluster. Changes sync to other
	servers automatically.
</p>

{#if data.error}
	<p class="text-destructive">{data.error}</p>
{:else}
	<form class="mb-6 card p-5" onsubmit={handleUpload}>
		<h2 class="mb-1 text-lg font-medium">Upload plugin</h2>
		<p class="mb-4 text-hint">Select a JAR with Plugin-Id and Plugin-Version manifest attributes.</p>

		<label class="flex flex-col gap-1">
			<span class="text-label">Plugin JAR</span>
			<input
				type="file"
				name="jar"
				accept=".jar,application/java-archive"
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

	<form onsubmit={handleSave}>
		<section class="card">
			<div class="card-header">
				<h2 class="text-lg font-medium">Installed plugins</h2>
			</div>

			{#if sortedPlugins.length === 0}
				<p class="p-5 text-hint">No plugins are registered.</p>
			{:else}
				<div class="overflow-x-auto">
					<table class="w-full text-sm">
						<thead class="border-b border-border text-left text-label">
							<tr>
								<th class="px-5 py-3 font-medium">Plugin</th>
								<th class="px-5 py-3 font-medium">Enabled</th>
								<th class="px-5 py-3 font-medium">Modified</th>
								<th class="px-5 py-3 font-medium"><span class="sr-only">Actions</span></th>
							</tr>
						</thead>
						<tbody class="divide-y divide-border">
							{#each sortedPlugins as plugin (plugin.name)}
								<tr>
									<td class="px-5 py-4">
										<p class="font-mono font-medium">{plugin.name}</p>
										<p class="text-hint">Version {plugin.version ?? '—'}</p>
										{#if !plugin.loaded}
											<p class="text-hint">Not loaded</p>
										{/if}
									</td>
									<td class="px-5 py-4">
										{#if plugin.name !== 'builtin'}
											<input
												type="checkbox"
												class="size-4 rounded border-border-input"
												disabled={submitting}
												aria-label="Enable {plugin.name}"
												bind:checked={draftEnabled[plugin.name!]}
											/>
										{:else}
											<span class="text-hint">Always on</span>
										{/if}
									</td>
									<td class="px-5 py-4 text-foreground">
										{#if plugin.dateModified}
											<time datetime={plugin.dateModified}>
												{new Date(plugin.dateModified).toLocaleString()}
											</time>
										{:else}
											—
										{/if}
									</td>
									<td class="px-5 py-4 text-right">
										{#if plugin.name !== 'builtin'}
											<button
												type="button"
												class="btn-ghost text-destructive"
												disabled={submitting}
												onclick={() => {
													deleteTarget = plugin;
													deleteOpen = true;
												}}
											>
												Delete
											</button>
										{/if}
									</td>
								</tr>
							{/each}
						</tbody>
					</table>
				</div>
			{/if}
		</section>

		<AuditSaveCard
			bind:auditComment
			required={auditRequired}
			requiredHint="Required when uploading, saving plugin changes, or deleting plugins."
			class="mt-6"
			{formError}
			{successMessage}
			{submitting}
			dirty={dirtyNames.length > 0}
			onreset={resetDraft}
		/>
	</form>
{/if}

<AppDialog
	bind:open={deleteOpen}
	title="Delete plugin"
	onclose={() => {
		deleteTarget = null;
	}}
>
	{#snippet description()}
		{#if deleteTarget}
			Remove <span class="font-mono">{deleteTarget.name}</span> from the database and this server?
		{/if}
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
</AppDialog>
