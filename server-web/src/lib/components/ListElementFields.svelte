<script lang="ts">
	import AdminFormError from '#lib/components/AdminFormError.svelte';
	import AuditCommentField from '#lib/components/AuditCommentField.svelte';
	import type { SchemaFormError } from '#lib/components/SchemaForm.svelte';
	import { t } from '#lib/i18n/catalog.js';

	let {
		id = $bindable(''),
		name = $bindable(''),
		description = $bindable(''),
		index = $bindable(0),
		aliases = $bindable(''),
		auditComment = $bindable(''),
		showId = false,
		error = undefined,
		auditRequired = false,
		disabled = false,
		class: className,
	}: {
		id?: string;
		name?: string;
		description?: string;
		index?: number;
		aliases?: string;
		auditComment?: string;
		showId?: boolean;
		error?: SchemaFormError;
		auditRequired?: boolean;
		disabled?: boolean;
		class?: string;
	} = $props();
</script>

<div class={['flex flex-col gap-3', className]}>
	{#if showId}
		<label class="flex flex-col gap-1">
			<span class="text-label">{t('web.common.id')}</span>
			<input class="input w-full font-mono" name="id" bind:value={id} required {disabled} />
		</label>
	{/if}
	<label class="flex flex-col gap-1">
		<span class="text-label">{t('web.common.name')}</span>
		<input class="input w-full" name="name" bind:value={name} required {disabled} />
	</label>
	<label class="flex flex-col gap-1">
		<span class="text-label">{t('web.common.description')}</span>
		<textarea class="input w-full" name="description" bind:value={description} required rows={2} {disabled}
		></textarea>
	</label>
	<label class="flex flex-col gap-1">
		<span class="text-label">{t('web.lists.element_index')}</span>
		<input class="input w-full" type="number" name="index" bind:value={index} {disabled} />
	</label>
	<label class="flex flex-col gap-1">
		<span class="text-label">{t('web.lists.element_aliases')}</span>
		<input
			class="input w-full"
			name="aliases"
			bind:value={aliases}
			placeholder="alias1, alias2"
			{disabled}
		/>
	</label>
	<AdminFormError {error} />
	<AuditCommentField bind:value={auditComment} required={auditRequired} {disabled} rows={2} />
</div>
