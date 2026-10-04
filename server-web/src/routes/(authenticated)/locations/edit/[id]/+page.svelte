<script lang="ts">
	import LocationEditor from '$lib/components/LocationEditor.svelte';

	let { data } = $props();

	const auditRequired = $derived(
		data.location?.kind === 'user' || data.location?.kind === 'group'
			? data.auditCommentRequired[data.location.kind].update
			: false
	);
</script>

<LocationEditor
	mode="edit"
	types={data.types}
	authProviders={data.authProviders}
	{auditRequired}
	location={data.location}
	user={data.user}
	loadError={data.loadError ?? data.error}
/>
