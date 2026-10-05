<script lang="ts">
	import { tApiErrorResponse } from '#lib/i18n/catalog.js';
	import type { SchemaFormError } from './SchemaForm.svelte';

	let {
		error = undefined,
		class: className,
	}: {
		error?: SchemaFormError;
		class?: string;
	} = $props();
</script>

{#if error}
	<p class={['text-sm text-destructive', className]} role="alert">{tApiErrorResponse(error)}</p>
{/if}
