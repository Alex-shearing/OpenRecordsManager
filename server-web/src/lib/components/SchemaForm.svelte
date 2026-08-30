<script lang="ts">
	import type { InputFormSchema, InputFormSchemaField } from '$lib/api/types.gen';
	import type { Snippet } from 'svelte';

	let {
		schema,
		values = $bindable({}),
		fieldErrors = {},
		formError = '',
		submitting = false,
		submitLabel = 'Submit',
		submittingLabel = 'Submitting...',
		idPrefix = 'form',
		disabled = false,
		before,
		after,
		onsubmit,
	}: {
		schema: InputFormSchema;
		values?: Record<string, string>;
		fieldErrors?: Record<string, string>;
		formError?: string;
		submitting?: boolean;
		submitLabel?: string;
		submittingLabel?: string;
		idPrefix?: string;
		disabled?: boolean;
		before?: Snippet;
		after?: Snippet;
		onsubmit?: (event: SubmitEvent) => void | Promise<void>;
	} = $props();

	async function handleSubmit(event: SubmitEvent) {
		event.preventDefault();
		await onsubmit?.(event);
	}

	function inputType(property: InputFormSchemaField): 'text' | 'password' | 'email' {
		if (property.writeOnly || property.format === 'password') {
			return 'password';
		}

		if (property.format === 'email') {
			return 'email';
		}

		return 'text';
	}
</script>

<form class="flex flex-col gap-4" onsubmit={handleSubmit} novalidate>
	{@render before?.()}

	{#each Object.entries(schema.properties) as [key, field] (key)}
		{@const required = schema.required.includes(key)}
		
		<label class="flex flex-col gap-1">
			<span>{field.title}</span>
			{#if field.description}
				<span class="text-sm text-gray-500">{field.description}</span>
			{/if}
			<input
				id="{idPrefix}-{key}"
				type={inputType(field)}
				name={key}
				bind:value={values[key]}
				{required}
				minlength={field.minLength ?? undefined}
				maxlength={field.maxLength ?? undefined}
				pattern={field.pattern ?? undefined}
				autocomplete={field.writeOnly ? 'current-password' : undefined}
				disabled={submitting}
				class="rounded border border-gray-300 px-3 py-2 dark:border-gray-600"
				aria-invalid={fieldErrors[key] ? 'true' : undefined}
				aria-describedby={fieldErrors[key] ? `${idPrefix}-${key}-error` : undefined}
			/>
			{#if fieldErrors[key]}
				<span id="{idPrefix}-{key}-error" class="text-sm text-red-600" role="alert">
					{fieldErrors[key]}
				</span>
			{/if}
		</label>
	{/each}

	{@render after?.()}

	{#if formError}
		<p class="text-sm text-red-600" role="alert">{formError}</p>
	{/if}

	<button
		type="submit"
		disabled={submitting || disabled}
		class="rounded bg-blue-600 px-4 py-2 text-white disabled:opacity-50"
	>
		{submitting ? submittingLabel : submitLabel}
	</button>
</form>
