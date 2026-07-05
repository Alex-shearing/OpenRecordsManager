<script lang="ts">
	import { client } from "$lib";

	let server = client.GET('/config/this_server');
</script>

<h1>Welcome to SvelteKit</h1>
<p>Visit <a href="https://svelte.dev/docs/kit">svelte.dev/docs/kit</a> to read the documentation</p>

{#await server}
    Loading...
{:then {data, error}} 
    {#if data?.data}
        {#each Object.entries(data.data) as [key, value]}
            <p>{key}: {value}</p>
        {/each}
    {/if}
{/await}