<script lang="ts">
	import { t } from '#lib/i18n/catalog.js';

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

<input
	{id}
	type="number"
	{step}
	bind:value
	{disabled}
	placeholder={t('web.config.value_placeholder')}
	class="input w-full"
	inputmode="numeric"
	pattern="[0-9.-]*"
/>
