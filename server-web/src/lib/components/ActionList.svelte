<script lang="ts">
	import type { ActionResponse } from '#lib/api/types.gen.js';
	import LocationActionDialog from '#lib/components/LocationActionDialog.svelte';
	import RecordActionDialog from '#lib/components/RecordActionDialog.svelte';
	import { t } from '#lib/i18n/catalog.js';
	import {
		locationActionDescription,
		locationActionName,
		recordActionDescription,
		recordActionName,
	} from '#lib/i18n/labels.js';

	let {
		actions,
		kind,
		targetId,
	}: {
		actions: ActionResponse[];
		kind: 'location' | 'record';
		targetId: string;
	} = $props();

	let selectedAction = $state<ActionResponse>();
	let actionOpen = $state(false);

	function openAction(action: ActionResponse) {
		selectedAction = action;
		actionOpen = true;
	}

	function closeActionDialog() {
		actionOpen = false;
		selectedAction = undefined;
	}
</script>

{#if actions.length > 0}
	<section class="mb-8">
		<h2 class="mb-4 text-lg font-medium">{t('web.common.actions')}</h2>
		<ul class="list-panel">
			{#each actions as action (action.id)}
				<li>
					<button
						type="button"
						class="list-panel-item flex w-full flex-col gap-1 text-left"
						onclick={() => openAction(action)}
					>
						<span class="font-medium">
							{kind === 'location' ? locationActionName(action.id) : recordActionName(action.id)}
						</span>
						<span class="text-hint">
							{kind === 'location' ? locationActionDescription(action.id) : recordActionDescription(action.id)}
						</span>
					</button>
				</li>
			{/each}
		</ul>
	</section>

	{#if kind === 'location'}
		<LocationActionDialog
			bind:open={actionOpen}
			locationId={targetId}
			action={selectedAction}
			onclose={closeActionDialog}
		/>
	{:else}
		<RecordActionDialog
			bind:open={actionOpen}
			recordId={targetId}
			action={selectedAction}
			onclose={closeActionDialog}
		/>
	{/if}
{/if}
