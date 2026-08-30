<script lang="ts">
	import { client, AuthController } from '$lib';
	import type { AuthProviderListResponse } from '$lib/api/types.gen';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import SchemaForm from './SchemaForm.svelte';

	let {
		inputProviders,
		redirectProviders
	}: {
		inputProviders: AuthProviderListResponse[];
		redirectProviders: AuthProviderListResponse[];
	} = $props();

	function providerLabel(provider: AuthProviderListResponse): string {
		return provider.name || provider.type.type;
	}

	let selectedProviderId = $state<string | undefined>(inputProviders.at(0)?.id);
	let values = $state<Record<string, string>>({});
	let fieldErrors = $state<Record<string, string>>({});
	let formError = $state('');
	let submitting = $state(false);

	let selectedProvider = $derived(
		inputProviders.find((provider) => provider.id === selectedProviderId)
	);

	async function handleSubmit() {
		if (!selectedProvider?.loginSchema) {
			return;
		}

		submitting = true;
		fieldErrors = {};
		formError = '';

		const { error } = await AuthController.login({
			path: { provider: selectedProvider.id },
			body: values
		});

		submitting = false;

		if (error) {
			fieldErrors = (error.errorData ?? {}) as Record<string, string>;
			formError = 'Authentication failed'
			return;
		}

		let redirect = page.url.searchParams.get('redirect') ?? '/';
		if (redirect.startsWith('/login')) {
			redirect = '/'
		}

		await goto(redirect);
	}
</script>

{#if inputProviders.length > 0}
	<section class="rounded-lg border border-gray-200 p-4 dark:border-gray-700">
		{#if selectedProvider?.loginSchema}
			<SchemaForm
				schema={selectedProvider.loginSchema}
				bind:values
				{fieldErrors}
				{formError}
				{submitting}
				submitLabel="Sign in"
				submittingLabel="Signing in..."
				idPrefix="login"
				disabled={!selectedProvider}
				onsubmit={handleSubmit}
			>
				{#snippet before()}
					{#if inputProviders.length > 1}
						<label class="flex flex-col gap-1">
							<span class="font-medium">Sign in with</span>
							<select
								bind:value={selectedProviderId}
								class="rounded border border-gray-300 bg-white px-3 py-2 dark:border-gray-600 dark:bg-gray-900"
								disabled={submitting}
							>
								{#each inputProviders as provider (provider.id)}
									<option value={provider.id}>{providerLabel(provider)}</option>
								{/each}
							</select>
						</label>
					{:else}
						<p class="text-sm text-gray-600 dark:text-gray-300">
							Sign in with <span class="font-medium">{providerLabel(selectedProvider)}</span>
						</p>
					{/if}
				{/snippet}
			</SchemaForm>
		{/if}
	</section>
{/if}

{#if redirectProviders.length > 0}
	<div class="mt-6 flex flex-col gap-3">
		{#if inputProviders.length > 0}
			<p class="text-sm text-gray-500">Or continue with</p>
		{/if}

		{#each redirectProviders as provider (provider.id)}
			<a
				href={`${client.getConfig().baseUrl || ''}/api/auth/redirect/${provider.id}`}
				class="inline-block rounded border border-gray-300 px-4 py-2 text-center hover:bg-gray-50 dark:border-gray-600"
			>
				Continue with {providerLabel(provider)}
			</a>
		{/each}
	</div>
{/if}
