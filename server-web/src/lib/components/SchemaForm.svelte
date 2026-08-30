<script lang="ts">
	import type { components } from '$lib/types/schema';

	type LoginJsonSchema = components['schemas']['InputFormSchema'];
	type JsonSchemaProperty = components['schemas']['InputFormSchemaField'];

	let {
		schema,
		values = $bindable<Record<string, string>>({}),
		fieldErrors = $bindable<Record<string, string>>({}),
		submitting = false,
		submitLabel = 'Submit',
		formError = '',
		onsubmit
	}: {
		schema: LoginJsonSchema;
		values?: Record<string, string>;
		fieldErrors?: Record<string, string>;
		submitting?: boolean;
		submitLabel?: string;
		formError?: string;
		onsubmit: (values: Record<string, string>) => void | Promise<void>;
	} = $props();

	function schemaFields(s: LoginJsonSchema) {
		const properties = s.properties ?? {};
		const required = new Set(s.required ?? []);

		return Object.entries(properties).map(([key, property]) => ({
			key,
			label: property.title ?? key,
			description: property.description,
			required: required.has(key),
			minLength: property.minLength,
			maxLength: property.maxLength,
			pattern: property.pattern,
			property
		}));
	}

	function inputType(property: JsonSchemaProperty): 'text' | 'password' | 'email' | 'number' {
		if (property.writeOnly || property.format === 'password') return 'password';
		if (property.format === 'email') return 'email';
		if (property.type === 'number' || property.type === 'integer') return 'number';
		return 'text';
	}

	let fields = $derived(schemaFields(schema));

	async function handleSubmit(event: SubmitEvent) {
		event.preventDefault();
		await onsubmit(values);
	}
</script>

<form class="flex flex-col gap-4" onsubmit={handleSubmit} novalidate>
	{#each fields as field (field.key)}
		<label class="flex flex-col gap-1">
			<span class="text-sm font-medium">{field.label}{field.required ? ' *' : ''}</span>
			{#if field.description}
				<span class="text-sm text-gray-500">{field.description}</span>
			{/if}
			<input
				id="schema-{field.key}"
				type={inputType(field.property)}
				name={field.key}
				bind:value={values[field.key]}
				required={field.required}
				minlength={field.minLength ?? undefined}
				maxlength={field.maxLength ?? undefined}
				pattern={field.pattern ?? undefined}
				autocomplete={field.property.writeOnly ? 'current-password' : undefined}
				disabled={submitting}
				class="rounded border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-900"
				aria-invalid={fieldErrors[field.key] ? 'true' : undefined}
			/>
			{#if fieldErrors[field.key]}
				<span class="text-sm text-red-600" role="alert">{fieldErrors[field.key]}</span>
			{/if}
		</label>
	{/each}

	{#if formError}
		<p class="text-sm text-red-600" role="alert">{formError}</p>
	{/if}

	<button
		type="submit"
		disabled={submitting}
		class="rounded bg-(--color-primary) px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
	>
		{submitting ? 'Working…' : submitLabel}
	</button>
</form>
