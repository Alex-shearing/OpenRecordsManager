<script lang="ts">
	import { resolve } from '$app/paths';
	import ActionList from '$lib/components/ActionList.svelte';
	import MonoId from '$lib/components/MonoId.svelte';
	import PageContent from '$lib/components/layout/PageContent.svelte';
	import PropertyDisplay from '$lib/components/PropertyDisplay.svelte';
	import { t, tApiErrorResponse } from '$lib/i18n/catalog';
	import { locationTypeName } from '$lib/i18n/labels';

	let { data } = $props();

	const pageTitle = $derived(data.user?.username ?? data.location?.name ?? t('web.locations.view_title'));

	function authProviderName(providerId: string | undefined) {
		if (!providerId) return undefined;
		return data.authProviders.find(provider => provider.id === providerId)?.name ?? providerId;
	}
</script>

<PageContent>
	{#if data.loadError}
		<p class="text-destructive">{tApiErrorResponse(data.loadError)}</p>
	{:else if !data.location}
		<p class="text-destructive">{t('web.locations.not_found')}</p>
	{:else}
		<div class="mb-6 flex flex-wrap items-start justify-between gap-4">
			<h1 class="text-2xl font-semibold">{pageTitle}</h1>
			<a href={resolve('/(authenticated)/locations/edit/[id]', { id: data.location.id })} class="btn-secondary">
				{t('web.common.edit')}
			</a>
		</div>

		<section class="card mb-8 p-4">
			<PropertyDisplay properties={data.location.properties} definitions={data.properties}>
				{#snippet before()}
					<div>
						<dt class="text-hint">{t('web.locations.id')}</dt>
						<dd><MonoId value={data.location.id} /></dd>
					</div>
					<div>
						<dt class="text-hint">{t('web.common.type')}</dt>
						<dd>
							{locationTypeName(data.location.type)}
							{#if data.location.kind === 'user'}
								({t('web.locations.kind_user')})
							{:else if data.location.kind === 'group'}
								({t('web.locations.kind_group')})
							{/if}
						</dd>
					</div>
					{#if data.location.kind === 'user' && data.user}
						<div>
							<dt class="text-hint">{t('web.locations.username')}</dt>
							<dd>{data.user.username}</dd>
						</div>
						<div>
							<dt class="text-hint">{t('web.locations.auth_provider')}</dt>
							<dd>
								{authProviderName(data.user.authProvider) ?? t('web.locations.auth_provider_none')}
							</dd>
						</div>
						<div>
							<dt class="text-hint">{t('web.locations.enabled')}</dt>
							<dd>{data.user.enabled ? t('web.common.yes') : t('web.common.no')}</dd>
						</div>
					{:else if data.location.kind === 'group'}
						<div>
							<dt class="text-hint">{t('web.locations.name')}</dt>
							<dd>{data.location.name}</dd>
						</div>
					{/if}
				{/snippet}
			</PropertyDisplay>
		</section>

		{#if data.location.kind === 'user'}
			<ActionList actions={data.actions} kind="user" targetId={data.location.id} />
		{/if}
	{/if}
</PageContent>
