<script lang="ts">
	import LocationEditor from '#lib/components/LocationEditor.svelte';

	let { data } = $props();
</script>

<LocationEditor
	mode="create"
	types={data.types}
	authProviders={data.authProviders}
	auditRequired={data.auditCommentRequired.create}
	loadError={data.error}
/>
