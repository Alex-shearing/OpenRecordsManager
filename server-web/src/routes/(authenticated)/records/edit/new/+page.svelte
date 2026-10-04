<script lang="ts">
	import RecordEditor from '$lib/components/RecordEditor.svelte';

	let { data } = $props();
</script>

<RecordEditor
	mode="create"
	types={data.types}
	auditRequired={data.auditCommentRequired.create}
	loadError={data.error}
/>
