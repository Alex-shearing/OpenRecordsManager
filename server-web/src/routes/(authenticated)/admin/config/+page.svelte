<script lang="ts">
	import { getClient } from '$lib';
</script>

{#await getClient().GET(`/api/config`)}
	Loading...
{:then { data, error }}
	{#if error}
		Failed to load
	{:else}
		<ul>
			{#each Object.entries(data.data) as [key, val]}
				<li>{key}: {val}</li>
			{/each}
		</ul>
	{/if}
{/await}
