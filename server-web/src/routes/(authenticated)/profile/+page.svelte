<script lang="ts">
	import { goto } from '$app/navigation';
	import { AuthController } from '$lib/api';
	import { getApiClient } from '$lib/api-client';
	import ActionList from '$lib/components/ActionList.svelte';
	import MonoId from '$lib/components/MonoId.svelte';
	import PageContent from '$lib/components/layout/PageContent.svelte';
	import PropertyDisplay from '$lib/components/PropertyDisplay.svelte';
	import { t, tApiErrorResponse } from '$lib/i18n/catalog';

	let { data } = $props();

	let loggingOut = $state(false);

	const propertyOrder = ['builtin:username', 'builtin:surname', 'builtin:given_name'];

	async function handleLogout() {
		loggingOut = true;
		await AuthController.logout({ client: getApiClient() });
		await goto('/login');
	}
</script>

<PageContent>
	<h1 class="mb-6 text-2xl font-semibold">{t('web.profile.title')}</h1>

	{#if data.error}
		<p class="text-destructive">{tApiErrorResponse(data.error)}</p>
	{:else if data.me}
		<section class="card mb-8 p-4">
			<h2 class="text-lg font-medium">{t('web.profile.account')}</h2>
			<PropertyDisplay
				properties={data.me.properties ?? {}}
				definitions={data.properties}
				order={propertyOrder}
			>
				{#snippet before()}
					<div>
						<dt class="text-hint">{t('web.profile.user_id')}</dt>
						<dd><MonoId value={data.me.id} /></dd>
					</div>
				{/snippet}
			</PropertyDisplay>
		</section>

		<ActionList actions={data.actions} kind="user" targetId={data.me.id} />

		<section class="card p-4">
			<h2 class="text-lg font-medium">{t('web.profile.sign_out')}</h2>
			<p class="mt-1 text-hint">{t('web.profile.sign_out_hint')}</p>
			<button type="button" class="btn-secondary mt-4" disabled={loggingOut} onclick={handleLogout}>
				{loggingOut ? t('web.profile.signing_out') : t('web.profile.sign_out')}
			</button>
		</section>
	{/if}
</PageContent>
