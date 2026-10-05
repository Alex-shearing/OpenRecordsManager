<script lang="ts">
	import type { SchemaFormError } from '#lib/components/SchemaForm.svelte';
	import { tApiErrorResponse } from '#lib/i18n/catalog.js';

	let {
		title,
		intro,
		error,
	}: {
		title: string;
		intro: string;
		error?: SchemaFormError | null;
	} = $props();
</script>

<h1 class="mb-2 text-2xl font-semibold">{title}</h1>
<p class="mb-6 text-hint">{intro}</p>

{#if error}
	<p class="text-destructive">{tApiErrorResponse(error)}</p>
{/if}
