<script lang="ts">
	import { client } from '$lib';

	let providers = client.GET('/api/auth/providers');
</script>

{#await providers}
	Loading...
{:then { data, error }}
	{#if error}
		<p>There was an error loading login options {error}</p>
	{:else}
		{#each data.data as provider}
			<p>{provider.id}: {provider.type.type}</p>
		{/each}
	{/if}
{/await}
