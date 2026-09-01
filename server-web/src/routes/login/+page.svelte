<script lang="ts">
	import BrandingHeader from '$lib/components/BrandingHeader.svelte';
	import LoginForm from '$lib/components/LoginForm.svelte';

	let { data } = $props();
</script>

<BrandingHeader branding={data.branding} showLogoOnMobile />

<main class="mx-auto w-full max-w-md px-4 py-8">
	<div class="card">
		<div class="card-header">
			<h1 class="text-2xl font-semibold">Sign in</h1>
		</div>

		<div class="card-body">
			{#if data.providersError}
				<p class="text-sm text-destructive">{data.providersError}</p>
			{:else if data.inputProviders.length === 0 && data.redirectProviders.length === 0}
				<p class="text-hint">No sign-in options are available.</p>
			{:else}
				<LoginForm inputProviders={data.inputProviders} redirectProviders={data.redirectProviders} />
			{/if}
		</div>
	</div>

	{#if data.branding.supportUrl}
		<p class="mt-4 text-center text-hint">
			<a href={data.branding.supportUrl} class="text-link">Need help?</a>
		</p>
	{/if}
</main>
