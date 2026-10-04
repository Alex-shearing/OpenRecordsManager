<script lang="ts">
	import { AuditController, AuditOperation } from '#lib/api/index.js';
	import { getApiClient } from '#lib/api-client.js';
	import AuditSaveCard from '#lib/components/AuditSaveCard.svelte';
	import MonoId from '#lib/components/MonoId.svelte';
	import { type SchemaFormError } from '#lib/components/SchemaForm.svelte';
	import {
		buildPolicyDraft,
		findChangedPolicies,
		formatEntityType,
		formatInstant,
		groupPoliciesByEntity,
		policyKey,
	} from '#lib/audit/audit-utils.js';
	import { t, tApiErrorResponse, tx } from '#lib/i18n/catalog.js';
	import { invalidateAll } from '$app/navigation';
	import toast from 'svelte-hot-french-toast';

	let { data } = $props();

	const groupedPolicies = $derived(groupPoliciesByEntity(data.policies));

	// svelte-ignore state_referenced_locally
	let draftPolicies = $state(buildPolicyDraft(data.policies));
	let auditComment = $state('');
	let submitting = $state(false);
	let formError = $state<SchemaFormError>();

	const changedPolicies = $derived(findChangedPolicies(data.policies, draftPolicies));
	const isDirty = $derived(changedPolicies.length > 0);

	$effect(() => {
		data.policies;
		draftPolicies = buildPolicyDraft(data.policies);
	});

	function resetDraft() {
		draftPolicies = buildPolicyDraft(data.policies);
		formError = undefined;
	}

	async function handleSave(event: SubmitEvent) {
		event.preventDefault();

		if (!isDirty) {
			return;
		}

		submitting = true;
		formError = undefined;

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
					formError = error;
					return;
				}
			}

			toast.success(
				changedPolicies.length === 1 ? t('web.audit.saved_one') : t('web.audit.saved_many', changedPolicies.length)
			);
			await invalidateAll();
		} finally {
			submitting = false;
		}
	}
</script>

<h1 class="mb-2 text-2xl font-semibold">{t('web.audit.title')}</h1>
<p class="mb-6 text-hint">{t('web.audit.intro')}</p>

{#if data.error}
	<p class="text-destructive">{tApiErrorResponse(data.error)}</p>
{:else}
	<section class="card mb-6">
		<div class="card-header">
			<h2 class="text-lg font-medium">{t('web.audit.local_server')}</h2>
		</div>

		{#if data.status}
			<dl class="grid gap-4 p-5 sm:grid-cols-2">
				<div>
					<dt class="text-label">{t('web.audit.state')}</dt>
					<dd class="mt-1">
						{#if data.status.auditEnabled}
							<span class="inline-flex items-center gap-2 text-foreground">
								<span class="size-2 rounded-full bg-emerald-500"></span>
								{t('web.audit.active')}
							</span>
						{:else}
							<span class="inline-flex items-center gap-2 text-destructive">
								<span class="size-2 rounded-full bg-destructive"></span>
								{t('web.audit.disabled')}
							</span>
							{#if data.status.auditDisabledReason}
								<p class="mt-1 text-sm text-hint">
									{tx(`web.audit.disabled.${data.status.auditDisabledReason}`, data.status.auditDisabledReason)}
								</p>
							{/if}
						{/if}
					</dd>
				</div>

				<div>
					<dt class="text-label">{t('web.audit.database_writable')}</dt>
					<dd class="mt-1 text-foreground">{data.status.primaryWritable ? t('web.common.yes') : t('web.common.no')}</dd>
				</div>

				<div>
					<dt class="text-label">{t('web.audit.pending_spool')}</dt>
					<dd class="mt-1 text-foreground">
						<span class:text-destructive={data.status.pendingSpoolCount > 0}>
							{data.status.pendingSpoolCount}
						</span>
					</dd>
				</div>

				<div>
					<dt class="text-label">{t('config.app.audit.archive-enabled.name')}</dt>
					<dd class="mt-1 text-foreground">{data.status.archiveEnabled ? t('web.common.yes') : t('web.common.no')}</dd>
				</div>

				<div>
					<dt class="text-label">{t('config.app.audit.spool-drain-interval-seconds.name')}</dt>
					<dd class="mt-1 text-foreground">
						{data.status.drainIntervalSeconds != null
							? t('web.audit.drain_interval_seconds', data.status.drainIntervalSeconds)
							: t('web.common.em_dash')}
					</dd>
				</div>

				<div>
					<dt class="text-label">{t('web.audit.last_probe')}</dt>
					<dd class="mt-1 text-foreground">{formatInstant(data.status.lastProbeAt)}</dd>
				</div>

				<div>
					<dt class="text-label">{t('web.audit.last_successful_write')}</dt>
					<dd class="mt-1 text-foreground">{formatInstant(data.status.lastSuccessfulWriteAt)}</dd>
				</div>

				<div>
					<dt class="text-label">{t('web.audit.last_drain_attempt')}</dt>
					<dd class="mt-1 text-foreground">{formatInstant(data.status.lastDrainAttemptAt)}</dd>
				</div>

				<div>
					<dt class="text-label">{t('web.audit.last_successful_drain')}</dt>
					<dd class="mt-1 text-foreground">{formatInstant(data.status.lastSuccessfulDrainAt)}</dd>
				</div>
			</dl>

			<p class="border-t border-border px-5 py-4 text-sm text-hint">
				{t('web.audit.master_switch_hint')}
				<a href="/admin/config#audit" class="text-primary underline-offset-2 hover:underline">
					{t('web.admin.configuration')}
				</a>.
			</p>
		{:else}
			<p class="p-5 text-hint">{t('web.audit.status_unavailable')}</p>
		{/if}
	</section>

	<form id="audit-save-form" onsubmit={handleSave}>
		<section class="card">
			<div class="card-header">
				<h2 class="text-lg font-medium">{t('web.audit.policies')}</h2>
				<p class="text-sm text-hint">{t('web.audit.policies_hint')}</p>
			</div>

			{#if groupedPolicies.length === 0}
				<p class="p-5 text-hint">{t('web.audit.empty')}</p>
			{:else}
				<div class="overflow-x-auto">
					<table class="w-full min-w-4xl text-sm">
						<thead class="border-b border-border text-left text-label">
							<tr>
								<th class="px-5 py-3 font-medium">{t('web.audit.entity')}</th>
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
										<p><MonoId value={entityType} muted /></p>
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
														<span>{t('web.common.enabled')}</span>
													</label>
													<label class="flex items-center gap-2">
														<input
															type="checkbox"
															class="size-4 rounded border-border-input"
															disabled={submitting}
															bind:checked={draftPolicies[key].requiresComment}
														/>
														<span>{t('web.audit.comment_toggle')}</span>
													</label>
												</div>
											{:else}
												<span class="text-hint">{t('web.common.em_dash')}</span>
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
			error={formError}
			{submitting}
			dirty={isDirty}
			onreset={resetDraft}
		/>
	</form>
{/if}
