<script lang="ts">
	import { AuthController } from '$lib/api';
	import type { SimpleAuthProviderResponse } from '$lib/api/types.gen';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import SchemaForm from './SchemaForm.svelte';
	import { getApiClient } from '$lib/api-client';

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
	let fieldErrors = $state<Record<string, string>>({});
	let formError = $state('');
	let submitting = $state(false);

	let selectedProvider = $derived(inputProviders.find(provider => provider.id === selectedProviderId));
	let postLoginRedirect = $derived(safeRelativePath(page.url.searchParams.get('redirect')));

	async function handleSubmit(event: SubmitEvent) {
		event.preventDefault();

		if (!selectedProvider?.loginSchema) {
			return;
		}

		submitting = true;
		fieldErrors = {};
		formError = '';

		const { error } = await AuthController.login({
			client: getApiClient(),
			path: { provider: selectedProvider.id },
			body: values,
		});

		submitting = false;

		if (error) {
			fieldErrors = (error.errorData ?? {}) as Record<string, string>;
			formError = 'Authentication failed';
			return;
		}

		await goto(safeRelativePath(page.url.searchParams.get('redirect')));
	}
</script>

{#if inputProviders.length > 0}
	{#if selectedProvider?.loginSchema}
		<form class="flex flex-col gap-4" onsubmit={handleSubmit} novalidate>
			<SchemaForm
				schema={selectedProvider.loginSchema}
				bind:values
				{fieldErrors}
				{formError}
				{submitting}
				idPrefix="login"
			>
				{#snippet before()}
					{#if inputProviders.length > 1}
						<label class="flex flex-col gap-1">
							<span class="text-label">Sign in with</span>
							<select bind:value={selectedProviderId} class="input" disabled={submitting}>
								{#each inputProviders as provider (provider.id)}
									<option value={provider.id}>{providerLabel(provider)}</option>
								{/each}
							</select>
						</label>
					{:else}
						<p class="text-hint">
							Sign in with <span class="font-medium text-foreground">
								{providerLabel(selectedProvider)}
							</span>
						</p>
					{/if}
				{/snippet}
			</SchemaForm>

			<button type="submit" class="btn-primary" disabled={submitting || !selectedProvider}>
				{submitting ? 'Signing in...' : 'Sign in'}
			</button>
		</form>
	{/if}
{/if}

{#if redirectProviders.length > 0}
	<div class="mt-6 flex flex-col gap-3">
		{#if inputProviders.length > 0}
			<p class="text-hint">Or continue with</p>
		{/if}

		<ul class="list-panel">
			{#each redirectProviders as provider (provider.id)}
				{@const base = `${getApiClient().getConfig().baseUrl || ''}/api/auth/redirect/${provider.id}`}
				{@const href =
					postLoginRedirect !== '/'
						? `${base}?redirect=${encodeURIComponent(postLoginRedirect)}`
						: base}
				<li>
					<a {href} class="list-panel-item text-center font-medium">
						Continue with {providerLabel(provider)}
					</a>
				</li>
			{/each}
		</ul>
	</div>
{/if}
