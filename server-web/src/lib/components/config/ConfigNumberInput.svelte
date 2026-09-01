<script lang="ts">
	let {
		id,
		value = $bindable<number>(),
		disabled = false,
		step = '1',
	}: {
		id: string;
		value: number;
		disabled?: boolean;
		step?: '1' | 'any';
	} = $props();
</script>

<input {id} type="number" {step} bind:value {disabled} class="input w-full" inputmode="numeric" pattern="[0-9.-]*" />
