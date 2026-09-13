<script lang="ts">
	let {
		value,
		muted = false,
	}: {
		value: string;
		muted?: boolean;
	} = $props();
</script>

<span class={['font-mono text-sm break-all', muted && 'text-hint']}>{value}</span>
