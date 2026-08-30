<script lang="ts">
	import { page } from '$app/state';
	import { getClient } from '$lib';
	import { formatValue } from '$lib/api';
	import type { components } from '$lib/types/schema';

	type AuditPolicy = components['schemas']['AuditPolicyResponse'];
	type AuditEvent = components['schemas']['AuditEventResponse'];
	type AuditStatus = components['schemas']['AuditStatusResponse'];

	const ENTITY_TYPES = [
		'record',
		'user',
		'record_type',
		'config',
		'list',
		'list_element',
		'object_property',
		'file_store',
		'auth_provider',
		'record_revision',
		'file_store_middleware',
		'template'
	] as const;

	let status = $state<AuditStatus | null>(null);
	let policies = $state<AuditPolicy[]>([]);
	let events = $state<AuditEvent[]>([]);
	let selectedEvent = $state<AuditEvent | null>(null);
	let error = $state('');
	let loading = $state(true);

	let targetType = $state<(typeof ENTITY_TYPES)[number]>('record');
	let targetId = $state('');
	let eventId = $state(page.url.searchParams.get('eventId') ?? '');
	let searching = $state(false);

	async function loadBase() {
		loading = true;
		const [st, pol] = await Promise.all([
			getClient().GET('/api/audit/status'),
			getClient().GET('/api/audit/policies')
		]);
		loading = false;
		if (st.error && pol.error) {
			error = st.error.error ?? 'Failed to load audit';
			return;
		}
		error = '';
		status = st.data?.data ?? null;
		policies = pol.data?.data ?? [];

		const eid = page.url.searchParams.get('eventId');
		if (eid) {
			eventId = eid;
			await loadEventById();
		}
	}

	$effect(() => {
		loadBase();
	});

	async function updatePolicy(policy: AuditPolicy, enabled: boolean, requiresComment: boolean) {
		if (!policy.entityType || !policy.operation) return;
		const { error: err } = await getClient().PUT('/api/audit/policies', {
			params: {
				query: { entityType: policy.entityType, operation: policy.operation }
			},
			body: { enabled, requiresComment }
		});
		if (err) {
			alert(err.error ?? 'Update failed');
			return;
		}
		const refreshed = await getClient().GET('/api/audit/policies');
		policies = refreshed.data?.data ?? [];
	}

	async function searchEvents(event: SubmitEvent) {
		event.preventDefault();
		searching = true;
		selectedEvent = null;
		const { data, error: err } = await getClient().GET('/api/audit/events', {
			params: {
				query: { targetType, targetId: targetId.trim(), limit: 50 }
			}
		});
		searching = false;
		if (err) {
			alert(err.error ?? 'Search failed');
			return;
		}
		events = data?.data ?? [];
	}

	async function loadEventById() {
		const id = eventId.trim();
		if (!id) return;
		const { data, error: err } = await getClient().GET('/api/audit/events/{id}', {
			params: { path: { id } }
		});
		if (err || !data?.data) {
			alert(err?.error ?? 'Event not found');
			return;
		}
		selectedEvent = data.data;
	}
</script>

<div class="flex flex-col gap-8">
	<h1 class="text-2xl font-semibold">Audit</h1>

	{#if loading}
		<p class="text-sm text-gray-500">Loading…</p>
	{:else if error}
		<p class="text-sm text-red-600">{error}</p>
	{:else}
		{#if status}
			<section class="rounded-lg border border-gray-200 p-4 text-sm dark:border-gray-700">
				<h2 class="mb-2 text-lg font-medium">Status</h2>
				<ul class="space-y-1">
					<li>Primary writable: {formatValue(status.primaryWritable)}</li>
					<li>Pending spool: {formatValue(status.pendingSpoolCount)}</li>
					<li>Last probe: {formatValue(status.lastProbeAt)}</li>
					<li>Last successful write: {formatValue(status.lastSuccessfulWriteAt)}</li>
					<li>Last drain attempt: {formatValue(status.lastDrainAttemptAt)}</li>
					<li>Last successful drain: {formatValue(status.lastSuccessfulDrainAt)}</li>
				</ul>
			</section>
		{/if}

		<section class="rounded-lg border border-gray-200 p-4 dark:border-gray-700">
			<h2 class="mb-3 text-lg font-medium">Policies</h2>
			{#if policies.length === 0}
				<p class="text-sm text-gray-500">No policies</p>
			{:else}
				<ul class="divide-y divide-gray-200 text-sm">
					{#each policies as policy (`${policy.entityType}-${policy.operation}`)}
						<li class="flex flex-wrap items-center justify-between gap-3 py-3">
							<div>
								<div class="font-medium">
									{policy.displayName ?? `${policy.entityType} / ${policy.operation}`}
								</div>
								{#if policy.description}
									<p class="text-gray-500">{policy.description}</p>
								{/if}
							</div>
							<div class="flex flex-wrap gap-3">
								<label class="flex items-center gap-1">
									<input
										type="checkbox"
										checked={Boolean(policy.enabled)}
										onchange={(e) =>
											updatePolicy(
												policy,
												(e.currentTarget as HTMLInputElement).checked,
												Boolean(policy.requiresComment)
											)}
									/>
									Enabled
								</label>
								<label class="flex items-center gap-1">
									<input
										type="checkbox"
										checked={Boolean(policy.requiresComment)}
										onchange={(e) =>
											updatePolicy(
												policy,
												Boolean(policy.enabled),
												(e.currentTarget as HTMLInputElement).checked
											)}
									/>
									Requires comment
								</label>
							</div>
						</li>
					{/each}
				</ul>
			{/if}
		</section>

		<section class="rounded-lg border border-gray-200 p-4 dark:border-gray-700">
			<h2 class="mb-3 text-lg font-medium">Events</h2>
			<form class="mb-4 flex flex-wrap gap-2" onsubmit={searchEvents}>
				<select bind:value={targetType} class="rounded border border-gray-300 px-3 py-2 text-sm">
					{#each ENTITY_TYPES as t (t)}
						<option value={t}>{t}</option>
					{/each}
				</select>
				<input
					bind:value={targetId}
					required
					placeholder="Target ID"
					class="rounded border border-gray-300 px-3 py-2 text-sm"
				/>
				<button
					type="submit"
					disabled={searching}
					class="rounded bg-(--color-primary) px-4 py-2 text-sm text-white disabled:opacity-50"
				>
					Search
				</button>
			</form>

			<form class="mb-4 flex flex-wrap gap-2" onsubmit={(e) => { e.preventDefault(); loadEventById(); }}>
				<input
					bind:value={eventId}
					placeholder="Event UUID"
					class="rounded border border-gray-300 px-3 py-2 text-sm"
				/>
				<button type="submit" class="rounded border border-gray-300 px-3 py-2 text-sm">
					Get event
				</button>
			</form>

			{#if selectedEvent}
				<div class="mb-4 rounded border border-gray-100 bg-gray-50 p-3 text-sm dark:bg-gray-900">
					<h3 class="font-medium">Event detail</h3>
					<pre class="mt-2 overflow-auto text-xs">{JSON.stringify(selectedEvent, null, 2)}</pre>
				</div>
			{/if}

			{#if events.length === 0}
				<p class="text-sm text-gray-500">No events loaded</p>
			{:else}
				<ul class="divide-y divide-gray-200 text-sm">
					{#each events as event (event.id)}
						<li class="py-2">
							<button
								type="button"
								class="text-left hover:underline"
								onclick={() => {
									selectedEvent = event;
									eventId = event.id ?? '';
								}}
							>
								<span class="font-medium">{event.operation}</span>
								<span class="text-gray-500"> {event.occurredAt}</span>
								{#if event.summary}
									<div class="text-gray-600">{event.summary}</div>
								{/if}
							</button>
						</li>
					{/each}
				</ul>
			{/if}
		</section>
	{/if}
</div>
