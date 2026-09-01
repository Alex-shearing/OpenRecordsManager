<script lang="ts">
	let {
		id,
		value = $bindable<string>(),
		disabled = false,
	}: {
		id: string;
		value: string;
		disabled?: boolean;
	} = $props();
</script>

<input {id} type="text" bind:value {disabled} class="input w-full font-mono text-sm" />
