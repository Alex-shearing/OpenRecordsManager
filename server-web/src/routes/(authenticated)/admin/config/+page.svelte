<script lang="ts">
	import { ConfigController } from '$lib';
	import ConfigSettingRow from '$lib/components/ConfigSettingRow.svelte';
	import {
		buildSavedValues,
		findChangedConfigs,
		groupConfigs
	} from '$lib/config/config-utils';
	import { invalidateAll } from '$app/navigation';

	let { data } = $props();

	const sections = $derived(groupConfigs(data.configs));
	const savedValues = $derived(buildSavedValues(data.configs));

	// svelte-ignore state_referenced_locally
	let draftValues = $state(buildSavedValues(data.configs));
	let auditComment = $state('');
	let submitting = $state(false);
	let formError = $state('');
	let successMessage = $state('');

	const changedConfigs = $derived(findChangedConfigs(data.configs, draftValues, savedValues));
	const isDirty = $derived(changedConfigs.length > 0);

	async function handleSubmit(event: SubmitEvent) {
		event.preventDefault();

		if (!isDirty) {
			return;
		}

		if (data.requiresAuditComment && !auditComment.trim()) {
			formError = 'An audit comment is required to update configuration.';
			return;
		}

		submitting = true;
		formError = '';
		successMessage = '';

		const headers = auditComment.trim()
			? { 'X-ORM-Audit-Comment': auditComment.trim() }
			: undefined;

		const { error } = await ConfigController.setConfigs({
			body: Object.fromEntries(changedConfigs.map(a => [a.key, draftValues[a.key].currentValue])),
			headers
		});

		submitting = false;

		if (error) {
			formError = error.error ?? 'Failed to save.';
			return;
		}

		successMessage =
			changedConfigs.length === 1
				? 'Saved 1 setting.'
				: `Saved ${changedConfigs.length} settings.`;
		auditComment = '';
		await invalidateAll();
	}

	function handleReset() {
		draftValues = buildSavedValues(data.configs);
		formError = '';
		successMessage = '';
	}
</script>

<h1 class="mb-2 text-2xl font-semibold">Configuration</h1>
<p class="mb-6 text-hint">
	Settings stored in the database. Environment variables and <code class="text-xs">config.yml</code> still
	override these values on each server. Server-only keys (<code class="text-xs">server.*</code>) are not
	shown here.
</p>

{#if data.error}
	<p class="text-destructive">{data.error}</p>
{:else if data.configs.length === 0}
	<p class="text-hint">No configuration settings are available.</p>
{:else}
	<form class="flex flex-col gap-6" onsubmit={handleSubmit}>
		{#each sections as section (section.group?.id ?? 'other')}
			<section class="card">
				<div class="card-header">
					<h2 class="text-lg font-medium">{section.group?.title ?? 'Other'}</h2>
				</div>
				<div class="divide-y divide-border">
					{#each section.items as config (config.key)}
						<ConfigSettingRow {config} bind:value={draftValues[config.key]} disabled={submitting} />
					{/each}
				</div>
			</section>
		{/each}

		<section class="card p-5">
			<label class="flex flex-col gap-1">
				<span class="text-label">Audit comment</span>
				<span class="text-hint">
					{data.requiresAuditComment
						? 'Required when saving configuration changes.'
						: 'Optional; recorded with each change.'}
				</span>
				<textarea
					bind:value={auditComment}
					required={data.requiresAuditComment}
					disabled={submitting}
					rows={3}
					class="input w-full"
				></textarea>
			</label>

			{#if formError}
				<p class="mt-3 text-sm text-destructive" role="alert">{formError}</p>
			{/if}
			{#if successMessage}
				<p class="mt-3 text-sm text-foreground">{successMessage}</p>
			{/if}

			<div class="mt-4 flex flex-wrap gap-2">
				<button type="submit" class="btn-primary" disabled={submitting || !isDirty}>
					{submitting ? 'Saving…' : 'Save changes'}
				</button>
				<button type="button" class="btn-secondary" disabled={submitting || !isDirty} onclick={handleReset}>
					Reset
				</button>
			</div>
		</section>
	</form>
{/if}
