<script lang="ts">
	let {
		id,
		value = $bindable<boolean>(),
		disabled = false
	}: {
		id: string;
		value: boolean;
		disabled?: boolean;
	} = $props();
</script>

<label class="flex items-center gap-2">
	<input
		{id}
		type="checkbox"
		class="size-4 rounded border-border-input"
		checked={value}
		{disabled}
		onchange={(event) => {
			value = event.currentTarget.checked;
		}}
	/>
	<span class="text-sm text-foreground">{value ? 'Enabled' : 'Disabled'}</span>
</label>
