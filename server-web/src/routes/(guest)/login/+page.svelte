<script lang="ts">
	import LoginForm from '$lib/components/LoginForm.svelte';
	import { page } from '$app/state';

	let { data } = $props();

	let authError = $derived(
		page.url.searchParams.get('error') === 'auth_failed'
			? 'Sign-in with the identity provider failed.'
			: null
	);
</script>

<div class="card">
	<div class="card-header">
		<h1 class="text-2xl font-semibold">Sign in</h1>
	</div>

	<div class="card-body">
		{#if authError}
			<p class="mb-4 text-sm text-destructive" role="alert">{authError}</p>
		{/if}
		{#if data.providersError}
			<p class="text-sm text-destructive">{data.providersError}</p>
		{:else if data.inputProviders.length === 0 && data.redirectProviders.length === 0}
			<p class="text-hint">No sign-in options are available.</p>
		{:else}
			<LoginForm inputProviders={data.inputProviders} redirectProviders={data.redirectProviders} />
		{/if}
	</div>
</div>
