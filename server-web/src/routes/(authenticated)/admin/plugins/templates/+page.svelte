<script lang="ts">
	import { TemplateController	} from '$lib';

	const types = TemplateController.getTemplateTypes();

	const registerTemplate = async (type: string, template: string) => {
		const { data, error } = await TemplateController.registerTemplate({
			path: { type, template },
			query: { includeDependencies: true }
		});

		if (error) {
			console.log('Failed to register template:', error);
			alert(`Failed to register template ${template}: ${error.error}`);
		} else {
			alert(`Template ${template} registered successfully!`);
		}
	};
</script>

<div class="flex flex-col gap-4">
	<h1 class="text-2xl font-bold">Templates</h1>
	{#await types}
		Loading...
	{:then { data, error }}
		{#if data}
			{#each data.data as type}
				<h1 class="text-2xl font-bold">{type}</h1>
				{#await TemplateController.getTemplatesForType({ path: { type } })}
					Loading...
				{:then { data, error }}
					{#if data?.data}
						{#each data.data as template}
							<button
								class="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
								onclick={() => registerTemplate(type, template)}
							>
								{template}
							</button>
						{/each}
					{/if}
				{/await}
			{/each}
		{:else}
			<p>Failed to load templates {error}</p>
		{/if}
	{/await}
</div>
