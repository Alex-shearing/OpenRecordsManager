<script lang="ts">
	import BrandingHeader from '$lib/components/BrandingHeader.svelte';
	import LoginForm from '$lib/components/LoginForm.svelte';

	let { data } = $props();
</script>

<BrandingHeader branding={data.branding} showLogoOnMobile />

<main class="mx-auto w-full max-w-md px-4 py-8">
	<div class="overflow-hidden rounded-lg border border-gray-200 bg-white shadow-sm">
		<div class="border-b border-gray-200 px-5 py-4">
			<h1 class="text-2xl font-semibold">Sign in</h1>
		</div>

		<div class="px-5 py-4">
			{#if data.providersError}
				<p class="text-sm text-red-600">{data.providersError}</p>
			{:else if data.inputProviders.length === 0 && data.redirectProviders.length === 0}
				<p class="text-sm text-gray-600">No sign-in options are available.</p>
			{:else}
				<LoginForm
					inputProviders={data.inputProviders}
					redirectProviders={data.redirectProviders}
				/>
			{/if}
		</div>
	</div>

	{#if data.branding.supportUrl}
		<p class="mt-4 text-center text-sm text-gray-600">
			<a href={data.branding.supportUrl} class="text-blue-600 hover:underline">Need help?</a>
		</p>
	{/if}
</main>
