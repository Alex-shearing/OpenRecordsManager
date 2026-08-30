<script lang="ts">
	import { page } from '$app/state';
	import { getClient } from '$lib';
	import { downloadBinary, formatValue, uploadRevision } from '$lib/api';
	import ActionRunner from '$lib/components/ActionRunner.svelte';
	import type { components } from '$lib/types/schema';

	type RecordResponse = components['schemas']['RecordResponse'];
	type AuditEvent = components['schemas']['AuditEventResponse'];

	let recordId = $derived(page.params.id ?? '');
	let record = $state<RecordResponse | null>(null);
	let error = $state('');
	let loading = $state(true);
	let versionInput = $state('1.0');
	let uploadError = $state('');
	let uploading = $state(false);
	let auditEvents = $state<AuditEvent[]>([]);
	let auditError = $state('');
	let fileInput: HTMLInputElement | undefined = $state();

	async function load() {
		loading = true;
		error = '';
		const { data, error: err } = await getClient().GET('/api/records/{id}', {
			params: { path: { id: recordId } }
		});
		loading = false;
		if (err || !data?.data) {
			error = err?.error ?? 'Failed to load record';
			record = null;
			return;
		}
		record = data.data;
		if (record.revisions.length) {
			const last = record.revisions[record.revisions.length - 1];
			const parts = last.split('.').map(Number);
			if (parts.length >= 2) {
				parts[parts.length - 1] += 1;
				versionInput = parts.join('.');
			} else {
				versionInput = `${last}.1`;
			}
		} else {
			versionInput = '1.0';
		}

		const audit = await getClient().GET('/api/audit/events', {
			params: {
				query: { targetType: 'record', targetId: recordId, limit: 50 }
			}
		});
		if (audit.error) {
			auditError = audit.error.error ?? 'Failed to load audit events';
			auditEvents = [];
		} else {
			auditError = '';
			auditEvents = audit.data?.data ?? [];
		}
	}

	$effect(() => {
		void recordId;
		load();
	});

	async function download(version: string) {
		try {
			await downloadBinary(`/api/records/${recordId}/${version}`, `record-${recordId}-${version}`);
		} catch (e) {
			uploadError = e instanceof Error ? e.message : 'Download failed';
		}
	}

	async function upload() {
		const file = fileInput?.files?.[0];
		if (!file || !versionInput.trim()) {
			uploadError = 'Choose a file and version';
			return;
		}
		uploading = true;
		uploadError = '';
		const result = await uploadRevision(recordId, versionInput.trim(), file);
		uploading = false;
		if (!result.ok) {
			uploadError = result.error ?? 'Upload failed';
			return;
		}
		if (fileInput) fileInput.value = '';
		await load();
	}
</script>

<div class="flex flex-col gap-6">
	{#if loading}
		<p class="text-sm text-gray-500">Loading…</p>
	{:else if error}
		<p class="text-sm text-red-600">{error}</p>
	{:else if record}
		<div>
			<h1 class="text-2xl font-semibold">{record.title || 'Record'}</h1>
			<p class="font-mono text-xs text-gray-500">{record.id}</p>
			<p class="mt-1 text-sm">
				Type:
				<a
					href="/record-types/{encodeURIComponent(record.type)}"
					class="text-(--color-primary) underline"
				>
					{record.type}
				</a>
			</p>
		</div>

		<section class="rounded-lg border border-gray-200 p-4 dark:border-gray-700">
			<h2 class="mb-3 text-lg font-medium">Properties</h2>
			{#if Object.keys(record.properties).length === 0}
				<p class="text-sm text-gray-500">No properties</p>
			{:else}
				<ul class="space-y-1 text-sm">
					{#each Object.entries(record.properties) as [key, val] (key)}
						<li>
							<span class="text-gray-500">{key}:</span>
							{formatValue(val)}
						</li>
					{/each}
				</ul>
			{/if}
		</section>

		<section class="rounded-lg border border-gray-200 p-4 dark:border-gray-700">
			<h2 class="mb-3 text-lg font-medium">Revisions</h2>
			{#if record.revisions.length === 0}
				<p class="mb-3 text-sm text-gray-500">No revisions yet</p>
			{:else}
				<ul class="mb-4 divide-y divide-gray-200 rounded border border-gray-100 text-sm">
					{#each record.revisions as version (version)}
						<li class="flex items-center justify-between px-3 py-2">
							<span class="font-mono">{version}</span>
							<button
								type="button"
								class="text-(--color-primary) underline"
								onclick={() => download(version)}
							>
								Download
							</button>
						</li>
					{/each}
				</ul>
			{/if}

			<div class="flex flex-col gap-2 sm:flex-row sm:items-end">
				<label class="flex flex-col gap-1 text-sm">
					<span>Version</span>
					<input
						bind:value={versionInput}
						class="rounded border border-gray-300 px-3 py-2 dark:border-gray-600"
						pattern={'^[0-9]+(\\.[0-9]+)*$'}
					/>
				</label>
				<label class="flex flex-col gap-1 text-sm">
					<span>File</span>
					<input type="file" bind:this={fileInput} class="text-sm" />
				</label>
				<button
					type="button"
					class="rounded bg-(--color-primary) px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
					disabled={uploading}
					onclick={upload}
				>
					{uploading ? 'Uploading…' : 'Upload revision'}
				</button>
			</div>
			{#if uploadError}
				<p class="mt-2 text-sm text-red-600">{uploadError}</p>
			{/if}
		</section>

		<ActionRunner kind="record" targetId={record.id} />

		<section class="rounded-lg border border-gray-200 p-4 dark:border-gray-700">
			<h2 class="mb-3 text-lg font-medium">Audit events</h2>
			{#if auditError}
				<p class="text-sm text-red-600">{auditError}</p>
			{:else if auditEvents.length === 0}
				<p class="text-sm text-gray-500">No events</p>
			{:else}
				<ul class="divide-y divide-gray-200 text-sm">
					{#each auditEvents as event (event.id)}
						<li class="py-2">
							<div class="flex flex-wrap gap-2">
								<span class="font-medium">{event.operation}</span>
								<span class="text-gray-500">{event.occurredAt}</span>
								{#if event.actorUsername}
									<span>by {event.actorUsername}</span>
								{/if}
							</div>
							{#if event.summary}
								<p class="text-gray-600">{event.summary}</p>
							{/if}
							{#if event.id}
								<a
									href="/admin/audit?eventId={event.id}"
									class="text-xs text-(--color-primary) underline"
								>
									Details
								</a>
							{/if}
						</li>
					{/each}
				</ul>
			{/if}
		</section>
	{/if}
</div>
