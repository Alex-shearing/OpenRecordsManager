<script lang="ts">
	import { ConfigController } from '$lib/api';
	import { getApiClient } from '$lib/api-client';
	import ConfigSettingRow from '$lib/components/ConfigSettingRow.svelte';
	import AuditSaveCard from '$lib/components/AuditSaveCard.svelte';
	import { buildSavedValues, findChangedConfigs, groupConfigs } from '$lib/config/config-utils';
	import { invalidateAll } from '$app/navigation';
	import toast from 'svelte-hot-french-toast';

	let { data } = $props();

	const sections = $derived(groupConfigs(data.configs));
	const savedValues = $derived(buildSavedValues(data.configs));

	// svelte-ignore state_referenced_locally
	let draftValues = $state(buildSavedValues(data.configs));
	let auditComment = $state('');
	let submitting = $state(false);
	let formError = $state('');

	const changedConfigs = $derived(findChangedConfigs(data.configs, draftValues, savedValues));
	const isDirty = $derived(changedConfigs.length > 0);

	async function handleSave(event: SubmitEvent) {
		event.preventDefault();

		if (!isDirty) {
			return;
		}

		submitting = true;
		formError = '';

		try {
			const headers = auditComment.trim() ? { 'X-ORM-Audit-Comment': auditComment.trim() } : undefined;

			const { error } = await ConfigController.setConfigs({
				client: getApiClient(),
				body: Object.fromEntries(changedConfigs.map(a => [a.key, draftValues[a.key].currentValue])),
				headers,
			});

			if (error) {
				formError = error.error ?? 'Failed to save.';
				return;
			}

			toast.success(changedConfigs.length === 1 ? 'Saved 1 setting.' : `Saved ${changedConfigs.length} settings.`);
			await invalidateAll();
		} finally {
			submitting = false;
		}
	}

	function handleReset() {
		draftValues = buildSavedValues(data.configs);
		formError = '';
	}
</script>

<h1 class="mb-2 text-2xl font-semibold">Configuration</h1>
<p class="mb-6 text-hint">
	Adjust configuration settings saved in the database below. Please note that other configuration locations may take
	precedence over these.
</p>

{#if data.error}
	<p class="text-destructive">{data.error}</p>
{:else if data.configs.length === 0}
	<p class="text-hint">No configuration settings are available.</p>
{:else}
	<form id="config-save-form" class="flex flex-col gap-6" onsubmit={handleSave}>
		{#each sections as section (section.group.id)}
			<section class="card scroll-mt-28" id={section.group.id}>
				<div class="card-header">
					<h2 class="text-lg font-medium">{section.group.title}</h2>
				</div>
				<div class="divide-y divide-border">
					{#each section.items as config (config.key)}
						<ConfigSettingRow {config} bind:value={draftValues[config.key]} disabled={submitting} />
					{/each}
				</div>
			</section>
		{/each}

		<AuditSaveCard
			form="config-save-form"
			bind:auditComment
			required={data.requiresAuditComment}
			requiredHint="Required when saving configuration changes."
			{formError}
			{submitting}
			dirty={isDirty}
			onreset={handleReset}
		/>
	</form>
{/if}
