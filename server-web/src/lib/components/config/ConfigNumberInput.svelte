<script lang="ts">
	let {
		id,
		value = $bindable<number | null>(),
		disabled = false,
		step = '1'
	}: {
		id: string;
		value: number | null;
		disabled?: boolean;
		step?: '1' | 'any';
	} = $props();

	let textValue = $state(value == null ? '' : String(value));

	$effect(() => {
		const next = value == null ? '' : String(value);
		if (textValue !== next) {
			textValue = next;
		}
	});

	function handleInput(event: Event) {
		const input = event.currentTarget as HTMLInputElement;
		textValue = input.value;
		if (input.value.trim() === '') {
			value = null;
			return;
		}
		const parsed = step === 'any' ? Number.parseFloat(input.value) : Number.parseInt(input.value, 10);
		value = Number.isNaN(parsed) ? null : parsed;
	}
</script>

<input {id} type="number" {step} value={textValue} {disabled} class="input w-full" oninput={handleInput} />
