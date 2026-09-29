<script lang="ts">
	import { t } from '$lib/i18n/catalog';

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

<input
	{id}
	type="text"
	bind:value
	{disabled}
	placeholder={t('web.config.value_placeholder')}
	class="input w-full font-mono text-sm"
/>
