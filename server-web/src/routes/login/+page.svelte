<script lang="ts">
	import { client } from '$lib';
	import type { components } from '$lib/types/schema';
	import { goto } from '$app/navigation';
	import { page } from '$app/state';

	type AuthProvider = components['schemas']['AuthProviderListResponse'];
	type LoginJsonSchema = components['schemas']['InputFormSchema'];
	type JsonSchemaProperty = NonNullable<LoginJsonSchema['properties']>[string];

	let providers = client.GET('/api/auth/providers');

	let values: Record<string, string> = $state({});
	let fieldErrors: Record<string, string> = $state({});
	let authErrors: Record<string, string> = $state({});
	let submitting: Record<string, boolean> = $state({});

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

	function inputType(property: JsonSchemaProperty): 'text' | 'password' | 'email' {
		if (property.writeOnly || property.format === 'password') {
			return 'password';
		}

		if (property.format === 'email') {
			return 'email';
		}

		return 'text';
	}

	async function submitLogin(provider: AuthProvider, event: SubmitEvent) {
		event.preventDefault();
		if (!provider.loginSchema) {
			return;
		}

		submitting[provider.id] = true;
		fieldErrors= {};
		authErrors[provider.id] = '';

		const body = values ?? {};
		const { data, error, response } = await client.POST('/api/auth/login/{provider}', {
			params: { path: { provider: provider.id } },
			body
		});

		submitting[provider.id] = false;

		if (response.status === 400) {
			return;
		}

		if (error || !data?.success) {
			authErrors[provider.id] = 'Authentication failed';
			return;
		}

		const redirect = page.url.searchParams.get('redirect') ?? '/';
		await goto(redirect);
	}
</script>

<h1 class="mb-6 text-2xl font-semibold">Sign in</h1>

{#await providers}
	<p>Loading login options...</p>
{:then { data, error }}
	{#if error}
		<p class="text-red-600">There was an error loading login options.</p>
	{:else if data?.data}
		<div class="flex flex-col gap-8">
			{#each data.data as provider (provider.id)}
				<section class="rounded-lg border border-gray-200 p-4 dark:border-gray-700">
					<h2 class="mb-4 text-lg font-medium">{provider.name}</h2>

					{#if provider.loginSchema}
						<form class="flex flex-col gap-4" onsubmit={(event) => submitLogin(provider, event)}>
							{#each schemaFields(provider.loginSchema) as field (field.key)}
								<label class="flex flex-col gap-1">
									<span>{field.label}</span>
									{#if field.description}
										<span class="text-sm text-gray-500">{field.description}</span>
									{/if}
									<input
										type={inputType(field.property)}
										name={field.key}
										bind:value={values[field.key]}
										required={field.required}
										minlength={field.minLength ?? undefined}
										maxlength={field.maxLength ?? undefined}
										pattern={field.pattern ?? undefined}
										class="rounded border border-gray-300 px-3 py-2 dark:border-gray-600"
									/>
									{#if fieldErrors[field.key]}
										<span class="text-sm text-red-600">{fieldErrors[field.key]}</span>
									{/if}
								</label>
							{/each}

							{#if authErrors[provider.id]}
								<p class="text-sm text-red-600">{authErrors[provider.id]}</p>
							{/if}

							<button
								type="submit"
								disabled={submitting[provider.id]}
								class="rounded bg-blue-600 px-4 py-2 text-white disabled:opacity-50"
							>
								{submitting[provider.id] ? 'Signing in...' : 'Sign in'}
							</button>
						</form>
					{:else}
						<a
							href="/api/auth/redirect/{provider.id}"
							class="inline-block rounded bg-blue-600 px-4 py-2 text-white"
						>
							Continue with {provider.type.type}
						</a>
					{/if}
				</section>
			{/each}
		</div>
	{/if}
{/await}
