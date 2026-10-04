<script lang="ts">
	import RecordEditor from '#lib/components/RecordEditor.svelte';

	let { data } = $props();
</script>

<RecordEditor
	mode="edit"
	types={data.types}
	auditRequired={data.auditCommentRequired.update}
	record={data.record}
	loadError={data.loadError ?? data.error}
/>
