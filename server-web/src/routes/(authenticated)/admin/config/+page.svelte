<script lang="ts">
	import { ConfigController } from '$lib';
</script>

{#await ConfigController.databaseRetrieve()}
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
