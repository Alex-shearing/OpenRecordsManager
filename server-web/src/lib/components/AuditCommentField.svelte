<script lang="ts">
	import { t } from '#lib/i18n/catalog.js';

	let {
		value = $bindable(''),
		required = false,
		disabled = false,
		rows = 3,
		class: className,
	}: {
		value?: string;
		required?: boolean;
		disabled?: boolean;
		rows?: number;
		class?: string;
	} = $props();

	const id = $props.id();
</script>

<label class={['flex flex-col gap-1', className]} for={id}>
	<span class="text-label">{t('web.common.audit_comment')}</span>
	<textarea
		{id}
		class="input w-full"
		name="audit-comment"
		bind:value
		{required}
		{disabled}
		{rows}
	></textarea>
</label>
