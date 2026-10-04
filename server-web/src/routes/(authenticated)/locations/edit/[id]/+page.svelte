<script lang="ts">
	import LocationEditor from '#lib/components/LocationEditor.svelte';

	let { data } = $props();
</script>

<LocationEditor
	mode="edit"
	types={data.types}
	authProviders={data.authProviders}
	auditRequired={data.auditCommentRequired.update}
	location={data.location}
	loadError={data.loadError ?? data.error}
/>
