<script lang="ts">
	import LocationEditor from '$lib/components/LocationEditor.svelte';

	let { data } = $props();

	let kind = $state<'user' | 'group' | undefined>();
	const auditRequired = $derived(
		kind ? data.auditCommentRequired[kind].create : false
	);
</script>

<LocationEditor
	mode="create"
	types={data.types}
	authProviders={data.authProviders}
	bind:kind
	{auditRequired}
	loadError={data.error}
/>
