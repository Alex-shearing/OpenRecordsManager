<script module lang="ts">
	import type { ApiFieldError } from '$lib/api/types.gen';

	/** Form-level error: wire field error shape, optionally with nested field errors. */
	export type SchemaFormError = ApiFieldError & {
		fieldErrors?: {
			[key: string]: ApiFieldError;
		};
	};
</script>

<script lang="ts">
	import type { InputFormSchema, InputFormSchemaField } from '$lib/api/types.gen';
	import { t, tApiErrorResponse } from '$lib/i18n/catalog';
	import type { Snippet } from 'svelte';

	let {
		schema,
		values = $bindable({}),
		error = undefined,
		submitting = false,
		idPrefix = 'form',
		before,
		after,
	}: {
		schema: InputFormSchema;
		values?: Record<string, string>;
		error?: SchemaFormError;
		submitting?: boolean;
		idPrefix?: string;
		before?: Snippet;
		after?: Snippet;
	} = $props();

	const fieldErrors = $derived(error?.fieldErrors ?? {});

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

<div class="flex flex-col gap-4">
	{@render before?.()}

	{#each schema.properties ? Object.entries(schema.properties) : [] as [key, field] (key)}
		{@const required = schema.required?.includes(key) && !field.writeOnly && field.format !== 'password'}

		<label class="flex flex-col gap-1">
			<span>{t(field.title)}</span>
			{#if field.description}
				<span class="text-hint">{t(field.description)}</span>
			{/if}
			{#if field.enum && field.enum.length > 0}
				<select
					id="{idPrefix}-{key}"
					name={key}
					bind:value={values[key]}
					{required}
					disabled={submitting}
					class="input w-full"
					aria-invalid={fieldErrors[key] ? 'true' : undefined}
					aria-describedby={fieldErrors[key] ? `${idPrefix}-${key}-error` : undefined}
				>
					<option value="" disabled={required}>{t('web.common.select')}</option>
					{#each field.enum as v (v)}
						<option value={v}>{v}</option>
					{/each}
				</select>
			{:else}
				<input
					id="{idPrefix}-{key}"
					type={inputType(field)}
					name={key}
					bind:value={values[key]}
					{required}
					minlength={field.minLength ?? undefined}
					maxlength={field.maxLength ?? undefined}
					pattern={field.pattern ?? undefined}
					autocomplete={field.writeOnly || field.format === 'password' ? 'current-password' : undefined}
					disabled={submitting}
					class="input w-full"
					aria-invalid={fieldErrors[key] ? 'true' : undefined}
					aria-describedby={fieldErrors[key] ? `${idPrefix}-${key}-error` : undefined}
					placeholder=" "
				/>
			{/if}
			{#if fieldErrors[key]}
				<span id="{idPrefix}-{key}-error" class="text-sm text-destructive" role="alert">
					{tApiErrorResponse(fieldErrors[key])}
				</span>
			{/if}
		</label>
	{/each}

	{@render after?.()}

	{#if error}
		<p class="text-sm text-destructive" role="alert">{tApiErrorResponse(error)}</p>
	{/if}
</div>
