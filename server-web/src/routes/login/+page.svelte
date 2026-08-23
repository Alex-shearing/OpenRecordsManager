<script lang="ts">
	import { client } from '$lib';
	import LoginForm from '$lib/components/LoginForm.svelte';
	import type { components } from '$lib/types/schema';

	type AuthProvider = components['schemas']['AuthProviderListResponse'];

	let providers = client.GET('/api/auth/providers');
</script>

<h1 class="mb-6 text-2xl font-semibold">Sign in</h1>

{#await providers}
	<p>Loading login options...</p>
{:then { data, error }}
	{#if error}
		<p class="text-red-600">There was an error loading login options.</p>
	{:else if data?.data}
		{@const inputProviders = data.data.filter((provider: AuthProvider) => provider.loginSchema)}
		{@const redirectProviders = data.data.filter((provider: AuthProvider) => !provider.loginSchema)}

		{#if inputProviders.length === 0 && redirectProviders.length === 0}
			<p class="text-gray-600">No sign-in options are available.</p>
		{:else}
			<LoginForm {inputProviders} {redirectProviders} />
		{/if}
	{/if}
{/await}
