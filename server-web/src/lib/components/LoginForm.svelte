<script lang="ts">
	import { getClient } from '$lib';
	import type { components } from '$lib/types/schema';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { onMount } from 'svelte';
	import { apiUrl } from '$lib/runtime-config';

	type AuthProvider = components['schemas']['AuthProviderListResponse'];
	type LoginJsonSchema = components['schemas']['InputFormSchema'];
	type JsonSchemaProperty = components['schemas']['InputFormSchemaField'];

	let {
		inputProviders,
		redirectProviders
	}: {
		inputProviders: AuthProvider[];
		redirectProviders: AuthProvider[];
	} = $props();

	function schemaFields(schema: LoginJsonSchema) {
		const properties = schema.properties ?? {};
		const required = new Set(schema.required ?? []);

		return Object.entries(properties).map(([key, property]) => ({
			key,
			label: property.title ?? key,
			description: property.description,
			required: required.has(key),
			minLength: property.minLength,
			maxLength: property.maxLength,
			pattern: property.pattern,
			property
		}));
	}

	function emptyValues(schema: LoginJsonSchema) {
		return Object.fromEntries(schemaFields(schema).map((field) => [field.key, '']));
	}

	function inputType(property: JsonSchemaProperty): 'text' | 'password' | 'email' {
		if (property.writeOnly || property.format === 'password') {
			return 'password';
		}

		if (property.format === 'email') {
			return 'email';
		}

		return 'text';
	}

	function providerLabel(provider: AuthProvider): string {
		return provider.name || provider.type.type;
	}

	let selectedProviderId = $state<string | null>(null);
	let values = $state<Record<string, string>>({});
	let fieldErrors = $state<Record<string, string>>({});
	let authError = $state('');
	let submitting = $state(false);

	let activeProviderId = $derived(selectedProviderId ?? inputProviders[0]?.id ?? '');
	let selectedProvider = $derived(
		inputProviders.find((provider) => provider.id === activeProviderId)
	);
	let fields = $derived(
		selectedProvider?.loginSchema ? schemaFields(selectedProvider.loginSchema) : []
	);
	let showProviderSelect = $derived(inputProviders.length > 1);

	function syncValuesToProvider(providerId: string) {
		const provider = inputProviders.find((item) => item.id === providerId);
		if (provider?.loginSchema) {
			values = emptyValues(provider.loginSchema);
		}
	}

	onMount(() => {
		syncValuesToProvider(activeProviderId);
	});

	function handleProviderChange(event: Event) {
		const select = event.currentTarget as HTMLSelectElement;
		selectedProviderId = select.value;
		syncValuesToProvider(select.value);
		fieldErrors = {};
		authError = '';
	}

	async function handleSubmit(event: SubmitEvent) {
		event.preventDefault();

		const provider = selectedProvider;
		if (!provider?.loginSchema) {
			return;
		}

		submitting = true;
		fieldErrors = {};
		authError = '';

		const { data, error } = await getClient().POST('/api/auth/login/{provider}', {
			params: { path: { provider: provider.id } },
			body: values
		});

		submitting = false;

		if (error) {
			fieldErrors = error.errorData || {};
			console.log(fieldErrors)
			return;
		}

		if (error || !data?.success) {
			authError = 'Authentication failed';
			return;
		}

		const redirect = page.url.searchParams.get('redirect') ?? '/';
		await goto(redirect);
	}
</script>

{#if inputProviders.length > 0}
	<section class="rounded-lg border border-gray-200 p-4 dark:border-gray-700">
		<form class="flex flex-col gap-4" onsubmit={handleSubmit} novalidate>
			{#if showProviderSelect}
				<label class="flex flex-col gap-1">
					<span class="font-medium">Sign in with</span>
					<select
						value={activeProviderId}
						onchange={handleProviderChange}
						class="rounded border border-gray-300 bg-white px-3 py-2 dark:border-gray-600 dark:bg-gray-900"
						disabled={submitting}
					>
						{#each inputProviders as provider (provider.id)}
							<option value={provider.id}>{providerLabel(provider)}</option>
						{/each}
					</select>
				</label>
			{:else if selectedProvider}
				<p class="text-sm text-gray-600 dark:text-gray-300">
					Sign in with <span class="font-medium">{providerLabel(selectedProvider)}</span>
				</p>
			{/if}

			{#each fields as field (field.key)}
				<label class="flex flex-col gap-1">
					<span>{field.label}</span>
					{#if field.description}
						<span class="text-sm text-gray-500">{field.description}</span>
					{/if}
					<input
						id="login-{field.key}"
						type={inputType(field.property)}
						name={field.key}
						bind:value={values[field.key]}
						required={field.required}
						minlength={field.minLength ?? undefined}
						maxlength={field.maxLength ?? undefined}
						pattern={field.pattern ?? undefined}
						autocomplete={field.property.writeOnly ? 'current-password' : undefined}
						disabled={submitting}
						class="rounded border border-gray-300 px-3 py-2 dark:border-gray-600"
						aria-invalid={fieldErrors[field.key] ? 'true' : undefined}
						aria-describedby={fieldErrors[field.key] ? `login-${field.key}-error` : undefined}
					/>
					{#if fieldErrors[field.key]}
						<span id="login-{field.key}-error" class="text-sm text-red-600" role="alert">
							{fieldErrors[field.key]}
						</span>
					{/if}
				</label>
			{/each}

			{#if authError}
				<p class="text-sm text-red-600" role="alert">{authError}</p>
			{/if}

			<button
				type="submit"
				disabled={submitting || !selectedProvider}
				class="rounded bg-blue-600 px-4 py-2 text-white disabled:opacity-50"
			>
				{submitting ? 'Signing in...' : 'Sign in'}
			</button>
		</form>
	</section>
{/if}

{#if redirectProviders.length > 0}
	<div class="mt-6 flex flex-col gap-3">
		{#if inputProviders.length > 0}
			<p class="text-sm text-gray-500">Or continue with</p>
		{/if}

		{#each redirectProviders as provider (provider.id)}
			<a
				href={apiUrl(`/api/auth/redirect/${provider.id}`)}
				class="inline-block rounded border border-gray-300 px-4 py-2 text-center hover:bg-gray-50 dark:border-gray-600"
			>
				Continue with {providerLabel(provider)}
			</a>
		{/each}
	</div>
{/if}
