<script lang="ts">
	import { AuthController } from '$lib/api';
	import type { SimpleAuthProviderResponse } from '$lib/api/types.gen';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import SchemaForm from './SchemaForm.svelte';
	import type { SchemaFormError } from './SchemaForm.svelte';
	import { getApiClient } from '$lib/api-client';
	import { t } from '$lib/i18n/catalog';

	let {
		inputProviders,
		redirectProviders,
	}: {
		inputProviders: SimpleAuthProviderResponse[];
		redirectProviders: SimpleAuthProviderResponse[];
	} = $props();

	function providerLabel(provider: SimpleAuthProviderResponse): string {
		return provider.name || provider.type.type;
	}

	function safeRelativePath(path: string | null | undefined): string {
		if (path == null || path.trim() === '') {
			return '/';
		}
		if (!path.startsWith('/') || path.startsWith('//') || path.includes('\\')) {
			return '/';
		}
		if (path.startsWith('/login')) {
			return '/';
		}
		return path;
	}

	// svelte-ignore state_referenced_locally
	let selectedProviderId = $state<string | undefined>(inputProviders.at(0)?.id);
	let values = $state<Record<string, string>>({});
	let error = $state<SchemaFormError>();
	let submitting = $state(false);

	let selectedProvider = $derived(inputProviders.find(provider => provider.id === selectedProviderId));
	let postLoginRedirect = $derived(safeRelativePath(page.url.searchParams.get('redirect')));

	async function handleSubmit(event: SubmitEvent) {
		event.preventDefault();

		if (!selectedProvider?.loginSchema) {
			return;
		}

		submitting = true;
		error = undefined;

		const { error: apiError } = await AuthController.login({
			client: getApiClient(),
			path: { provider: selectedProvider.id },
			body: values,
		});

		submitting = false;

		if (apiError) {
			error = apiError;
			return;
		}

		await goto(safeRelativePath(page.url.searchParams.get('redirect')));
	}
</script>

{#if inputProviders.length > 0}
	{#if selectedProvider?.loginSchema}
		<form class="flex flex-col gap-4" onsubmit={handleSubmit} novalidate>
			<SchemaForm schema={selectedProvider.loginSchema} bind:values {error} {submitting} idPrefix="login">
				{#snippet before()}
					{#if inputProviders.length > 1}
						<label class="flex flex-col gap-1">
							<span class="text-label">{t('web.login.sign_in_with')}</span>
							<select bind:value={selectedProviderId} class="input" disabled={submitting}>
								{#each inputProviders as provider (provider.id)}
									<option value={provider.id}>{providerLabel(provider)}</option>
								{/each}
							</select>
						</label>
					{:else}
						<p class="text-hint">
							{t('web.login.sign_in_with')}
							<span class="font-medium text-foreground">
								{providerLabel(selectedProvider)}
							</span>
						</p>
					{/if}
				{/snippet}
			</SchemaForm>

			<button type="submit" class="btn-primary" disabled={submitting || !selectedProvider}>
				{submitting ? t('web.login.signing_in') : t('web.login.title')}
			</button>
		</form>
	{/if}
{/if}

{#if redirectProviders.length > 0}
	<div class="mt-6 flex flex-col gap-3">
		{#if inputProviders.length > 0}
			<p class="text-hint">{t('web.login.or_continue_with')}</p>
		{/if}

		<ul class="list-panel">
			{#each redirectProviders as provider (provider.id)}
				{@const base = `${getApiClient().getConfig().baseUrl || ''}/api/auth/redirect/${provider.id}`}
				{@const href = postLoginRedirect !== '/' ? `${base}?redirect=${encodeURIComponent(postLoginRedirect)}` : base}
				<li>
					<a {href} class="list-panel-item text-center font-medium">
						{t('web.login.continue_with', providerLabel(provider))}
					</a>
				</li>
			{/each}
		</ul>
	</div>
{/if}
