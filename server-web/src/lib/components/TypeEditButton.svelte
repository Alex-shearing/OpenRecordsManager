<script lang="ts">
	import { t } from '#lib/i18n/catalog.js';
	import { isBuiltinResourceId } from '#lib/types/typeAdmin.js';

	let {
		id,
		disabled = false,
		onclick,
	}: {
		id: string;
		disabled?: boolean;
		onclick: () => void;
	} = $props();

	const builtin = $derived(isBuiltinResourceId(id));
</script>

<button
	type="button"
	class="btn-ghost"
	disabled={disabled || builtin}
	title={builtin ? t('web.types.builtin_immutable') : undefined}
	{onclick}
>
	{t('web.common.edit')}
</button>
