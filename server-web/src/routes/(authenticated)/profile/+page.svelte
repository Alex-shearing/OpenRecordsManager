<script lang="ts">
	import { AuthController } from '$lib/api';
	import type { ActionResponse } from '$lib/api/types.gen';
	import { getApiClient } from '$lib/api-client';
	import MonoId from '$lib/components/MonoId.svelte';
	import PageContent from '$lib/components/layout/PageContent.svelte';
	import UserActionDialog from '$lib/components/UserActionDialog.svelte';
	import { objectPropertyName, userActionDescription, userActionName } from '$lib/i18n/labels';
	import { t } from '$lib/i18n/catalog';
	import { goto } from '$app/navigation';

	let { data } = $props();

	let loggingOut = $state(false);
	let selectedAction = $state<ActionResponse | null>(null);
	let actionOpen = $state(false);

	function openAction(action: ActionResponse) {
		selectedAction = action;
		actionOpen = true;
	}

	function closeActionDialog() {
		actionOpen = false;
		selectedAction = null;
	}

	async function handleLogout() {
		loggingOut = true;
		await AuthController.logout({ client: getApiClient() });
		await goto('/login');
	}

	const propertyOrder = ['builtin:username', 'builtin:surname', 'builtin:given_name'];

	const sortedProperties = $derived(
		Object.entries(data.me.properties ?? {}).toSorted(([a], [b]) => {
			const ai = propertyOrder.indexOf(a);
			const bi = propertyOrder.indexOf(b);
			if (ai !== -1 || bi !== -1) {
				if (ai === -1) return 1;
				if (bi === -1) return -1;
				return ai - bi;
			}
			return a.localeCompare(b);
		})
	);
</script>

<PageContent>
	<h1 class="mb-6 text-2xl font-semibold">{t('web.profile.title')}</h1>

	{#if data.error}
		<p class="text-destructive">{data.error}</p>
	{:else if data.me}
		<section class="card mb-8 p-4">
			<h2 class="text-lg font-medium">{t('web.profile.account')}</h2>
			<dl class="mt-4 grid gap-3 sm:grid-cols-2">
				<div>
					<dt class="text-hint">{t('web.profile.user_id')}</dt>
					<dd><MonoId value={data.me.id} /></dd>
				</div>
				{#each sortedProperties as [key, value] (key)}
					<div>
						<dt class="text-hint">{objectPropertyName(key)}</dt>
						<dd>{String(value)}</dd>
					</div>
				{/each}
			</dl>
		</section>

		{#if data.actions.length > 0}
			<section class="mb-8">
				<h2 class="mb-4 text-lg font-medium">{t('web.profile.actions')}</h2>
				<ul class="list-panel">
					{#each data.actions as action (action.id)}
						<li>
							<button
								type="button"
								class="list-panel-item flex w-full flex-col gap-1 text-left"
								onclick={() => openAction(action)}
							>
								<span class="font-medium">{userActionName(action.id)}</span>
								<span class="text-hint">{userActionDescription(action.id)}</span>
							</button>
						</li>
					{/each}
				</ul>
			</section>
		{/if}

		<section class="card p-4">
			<h2 class="text-lg font-medium">{t('web.profile.sign_out')}</h2>
			<p class="mt-1 text-hint">{t('web.profile.sign_out_hint')}</p>
			<button type="button" class="btn-secondary mt-4" disabled={loggingOut} onclick={handleLogout}>
				{loggingOut ? t('web.profile.signing_out') : t('web.profile.sign_out')}
			</button>
		</section>

		<UserActionDialog bind:open={actionOpen} userId={data.me.id} action={selectedAction} onclose={closeActionDialog} />
	{/if}
</PageContent>
