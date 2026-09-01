<script lang="ts">
	import LoginForm from '$lib/components/LoginForm.svelte';

	let { data } = $props();
</script>

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
