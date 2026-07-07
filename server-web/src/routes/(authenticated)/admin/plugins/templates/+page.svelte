<script lang="ts">
	import { client } from "$lib";

    const types = client.GET('/api/templates');

    const registerTemplate = async (type: string, template: string) => {
        try {
            const {data, error} = await client.POST(`/api/templates/{type}/{template}/register`, {
                params: {
                    path: { type, template },
                    query: { includeDependencies: true }
                }
            });


            if (data) {
                alert(`Template ${template} registered successfully!`);
            } else {
                console.log('Failed to register template:', error);
                alert(`Failed to register template ${template}: ${error.error}`);
            }
        } catch (error) {
            console.error('Error registering template:', error);
            alert(`aaaaaaa Failed to register template ${template}: ${error}`);
        }
    };
</script>

<div class="flex flex-col gap-4">
    <h1 class="text-2xl font-bold">Templates</h1>
    {#await types}
        Loading...
    {:then {data, error}} 
        {#if data}
            {#each data.data as type}
                <h1 class="text-2xl font-bold">{type}</h1>
                {#await client.GET(`/api/templates/{type}`, { params: { path: { type } } })}
                    Loading...
                {:then {data, error}} 
                    {#if data?.data}
                        {#each data.data as template}
                            <button class="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded" onclick={() => registerTemplate(type, template)}>
                                {template}
                            </button>
                        {/each}
                    {/if}
                    
                {/await}
            {/each}
        {:else}
            <p>Failed to load templates {error.error}</p>
        {/if}
    {/await}
</div>