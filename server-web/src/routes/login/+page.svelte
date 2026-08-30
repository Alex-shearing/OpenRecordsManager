<script lang="ts">
	import { AuthController } from '$lib';
	import LoginForm from '$lib/components/LoginForm.svelte';
	import type { AuthProviderListResponse } from '$lib/api/types.gen';

	type AuthProvider = AuthProviderListResponse;

	let providers = AuthController.providersListAll();
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
