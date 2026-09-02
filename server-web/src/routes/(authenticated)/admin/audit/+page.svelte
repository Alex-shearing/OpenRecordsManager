<script lang="ts">
	import { AuditController, AuditOperation } from '$lib/api';
	import { getApiClient } from '$lib/api-client';
	import AuditSaveCard from '$lib/components/AuditSaveCard.svelte';
	import {
		buildPolicyDraft,
		findChangedPolicies,
		formatDisabledReason,
		formatEntityType,
		formatInstant,
		groupPoliciesByEntity,
		policyKey,
	} from '$lib/audit/audit-utils';
	import { invalidateAll } from '$app/navigation';
	import toast from 'svelte-hot-french-toast';

	let { data } = $props();

	const groupedPolicies = $derived(groupPoliciesByEntity(data.policies));

	// svelte-ignore state_referenced_locally
	let draftPolicies = $state(buildPolicyDraft(data.policies));
	let auditComment = $state('');
	let submitting = $state(false);
	let formError = $state('');

	const changedPolicies = $derived(findChangedPolicies(data.policies, draftPolicies));
	const isDirty = $derived(changedPolicies.length > 0);

	$effect(() => {
		data.policies;
		draftPolicies = buildPolicyDraft(data.policies);
	});

	function resetDraft() {
		draftPolicies = buildPolicyDraft(data.policies);
		formError = '';
	}

	async function handleSave(event: SubmitEvent) {
		event.preventDefault();

		if (!isDirty) {
			return;
		}

		submitting = true;
		formError = '';

		try {
			const client = getApiClient();

			for (const policy of changedPolicies) {
				const entityType = policy.entityType ?? '';
				const operation = policy.operation;
				if (!entityType || !operation) {
					continue;
				}
				const key = policyKey(entityType, operation);
				const draft = draftPolicies[key];

				const { error } = await AuditController.updateAuditPolicy({
					client,
					query: {
						entityType,
						operation,
					},
					body: {
						enabled: draft.enabled,
						requiresComment: draft.requiresComment,
					},
				});

				if (error) {
					formError = error.error ?? `Failed to update ${entityType} ${operation}.`;
					await invalidateAll();
					return;
				}
			}

			toast.success(
				changedPolicies.length === 1 ? 'Saved 1 audit policy.' : `Saved ${changedPolicies.length} audit policies.`
			);
			await invalidateAll();
		} finally {
			submitting = false;
		}
	}
</script>

<h1 class="mb-2 text-2xl font-semibold">Audit</h1>
<p class="mb-6 text-hint">Review audit subsystem health and configure which operations require logging or comments.</p>

{#if data.error}
	<p class="text-destructive">{data.error}</p>
{:else}
	<section class="card mb-6">
		<div class="card-header">
			<h2 class="text-lg font-medium">This server</h2>
		</div>

		{#if data.status}
			<dl class="grid gap-4 p-5 sm:grid-cols-2">
				<div>
					<dt class="text-label">Audit state</dt>
					<dd class="mt-1">
						{#if data.status.auditEnabled}
							<span class="inline-flex items-center gap-2 text-foreground">
								<span class="size-2 rounded-full bg-emerald-500"></span>
								Active
							</span>
						{:else}
							<span class="inline-flex items-center gap-2 text-destructive">
								<span class="size-2 rounded-full bg-destructive"></span>
								Disabled
							</span>
							{#if data.status.auditDisabledReason}
								<p class="mt-1 text-sm text-hint">
									{formatDisabledReason(data.status.auditDisabledReason)}
								</p>
							{/if}
						{/if}
					</dd>
				</div>

				<div>
					<dt class="text-label">Database writable</dt>
					<dd class="mt-1 text-foreground">{data.status.primaryWritable ? 'Yes' : 'No'}</dd>
				</div>

				<div>
					<dt class="text-label">Pending spool events</dt>
					<dd class="mt-1 text-foreground">
						<span class:text-destructive={data.status.pendingSpoolCount > 0}>
							{data.status.pendingSpoolCount}
						</span>
					</dd>
				</div>

				<div>
					<dt class="text-label">Archive enabled</dt>
					<dd class="mt-1 text-foreground">{data.status.archiveEnabled ? 'Yes' : 'No'}</dd>
				</div>

				<div>
					<dt class="text-label">Drain interval</dt>
					<dd class="mt-1 text-foreground">{data.status.drainIntervalSeconds ?? '—'}s</dd>
				</div>

				<div>
					<dt class="text-label">Last probe</dt>
					<dd class="mt-1 text-foreground">{formatInstant(data.status.lastProbeAt)}</dd>
				</div>

				<div>
					<dt class="text-label">Last successful write</dt>
					<dd class="mt-1 text-foreground">{formatInstant(data.status.lastSuccessfulWriteAt)}</dd>
				</div>

				<div>
					<dt class="text-label">Last drain attempt</dt>
					<dd class="mt-1 text-foreground">{formatInstant(data.status.lastDrainAttemptAt)}</dd>
				</div>

				<div>
					<dt class="text-label">Last successful drain</dt>
					<dd class="mt-1 text-foreground">{formatInstant(data.status.lastSuccessfulDrainAt)}</dd>
				</div>
			</dl>

			<p class="border-t border-border px-5 py-4 text-sm text-hint">
				Master switch and spool settings are managed on
				<a href="/admin/config#audit" class="text-primary underline-offset-2 hover:underline">Configuration</a>.
			</p>
		{:else}
			<p class="p-5 text-hint">Audit status is unavailable.</p>
		{/if}
	</section>

	<form id="audit-save-form" onsubmit={handleSave}>
		<section class="card">
			<div class="card-header">
				<h2 class="text-lg font-medium">Audit policies</h2>
				<p class="text-sm text-hint">Each cell has Enabled and Comment toggles for the operation.</p>
			</div>

			{#if groupedPolicies.length === 0}
				<p class="p-5 text-hint">No audit policies are available.</p>
			{:else}
				<div class="overflow-x-auto">
					<table class="w-full min-w-4xl text-sm">
						<thead class="border-b border-border text-left text-label">
							<tr>
								<th class="px-5 py-3 font-medium">Entity</th>
								{#each Object.keys(AuditOperation) as operation (operation)}
									<th class="px-5 py-3 font-medium">{operation}</th>
								{/each}
							</tr>
						</thead>
						<tbody class="divide-y divide-border">
							{#each groupedPolicies as [entityType, operations] (entityType)}
								<tr>
									<td class="px-5 py-4 align-top">
										<p class="font-medium">{formatEntityType(entityType)}</p>
										<p class="font-mono text-xs text-hint">{entityType}</p>
									</td>
									{#each Object.keys(AuditOperation) as operation (operation)}
										{@const policy = operations.get(operation)}
										{@const key = policyKey(entityType, operation)}
										<td class="px-5 py-4 align-top">
											{#if policy && draftPolicies[key]}
												<div class="flex flex-col gap-2">
													<label class="flex items-center gap-2">
														<input
															type="checkbox"
															class="size-4 rounded border-border-input"
															disabled={submitting}
															bind:checked={draftPolicies[key].enabled}
														/>
														<span>Enabled</span>
													</label>
													<label class="flex items-center gap-2">
														<input
															type="checkbox"
															class="size-4 rounded border-border-input"
															disabled={submitting}
															bind:checked={draftPolicies[key].requiresComment}
														/>
														<span>Comment</span>
													</label>
												</div>
											{:else}
												<span class="text-hint">—</span>
											{/if}
										</td>
									{/each}
								</tr>
							{/each}
						</tbody>
					</table>
				</div>
			{/if}
		</section>

		<AuditSaveCard
			form="audit-save-form"
			bind:auditComment
			required={false}
			requiredHint=""
			optionalHint="Optional; not required for audit policy changes."
			{formError}
			{submitting}
			dirty={isDirty}
			onreset={resetDraft}
		/>
	</form>
{/if}
