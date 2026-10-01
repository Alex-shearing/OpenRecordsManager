<script lang="ts">
	import { TemplateController } from '$lib/api';
	import { getApiClient } from '$lib/api-client';
	import MonoId from '$lib/components/MonoId.svelte';
	import { type SchemaFormError } from '$lib/components/SchemaForm.svelte';
	import { t, tApiErrorResponse } from '$lib/i18n/catalog';

	let { data } = $props();

	let selected = $state<Set<string>>(new Set());
	let registering = $state(false);
	let formError = $state<SchemaFormError>();
	let successMessage = $state('');

	function rowKey(type: string, templateId: string) {
		return `${type}:${templateId}`;
	}

	const selectedRows = $derived(data.templates.filter(row => selected.has(rowKey(row.type, row.id))));

	const allSelected = $derived(
		data.templates.length > 0 && data.templates.every(row => selected.has(rowKey(row.type, row.id)))
	);

	function setSelected(key: string, checked: boolean) {
		const next = new Set(selected);
		if (checked) {
			next.add(key);
		} else {
			next.delete(key);
		}
		selected = next;
	}

	function setAllSelected(checked: boolean) {
		selected = checked ? new Set(data.templates.map(row => rowKey(row.type, row.id))) : new Set();
	}

	async function handleRegister(event: SubmitEvent) {
		event.preventDefault();

		if (selectedRows.length === 0) {
			formError = { error: 'web.templates.select_at_least_one' };
			return;
		}

		registering = true;
		formError = undefined;
		successMessage = '';

		const registeredKeys = new Set<string>();

		for (const row of selectedRows) {
			const key = rowKey(row.type, row.id);
			const { error } = await TemplateController.registerTemplate({
				client: getApiClient(),
				path: { type: row.type, template: row.id },
				query: { includeDependencies: true },
			});

			if (error) {
				registering = false;
				selected = new Set([...selected].filter(k => !registeredKeys.has(k)));
				formError = error;
				if (registeredKeys.size > 0) {
					successMessage = t('web.templates.registered_partial', registeredKeys.size);
				}
				return;
			}

			registeredKeys.add(key);
		}

		selected = new Set([...selected].filter(key => !registeredKeys.has(key)));
		registering = false;
		successMessage = t('web.templates.registered_count', registeredKeys.size);
	}
</script>

<h1 class="mb-2 text-2xl font-semibold">{t('web.templates.title')}</h1>
<p class="mb-6 text-hint">{t('web.templates.intro')}</p>

{#if data.error}
	<p class="text-destructive">{tApiErrorResponse(data.error)}</p>
{:else if data.templates.length === 0}
	<p class="text-hint">{t('web.templates.empty')}</p>
{:else}
	<form onsubmit={handleRegister}>
		<section class="card">
			<div class="overflow-x-auto">
				<table class="w-full text-sm">
					<thead class="border-b border-border text-left text-label">
						<tr>
							<th class="px-5 py-3 font-medium">
								<input
									type="checkbox"
									class="size-4 rounded border-border-input"
									checked={allSelected}
									disabled={registering}
									aria-label={t('web.templates.select_all')}
									onchange={event => setAllSelected(event.currentTarget.checked)}
								/>
							</th>
							<th class="px-5 py-3 font-medium">{t('web.templates.col_type')}</th>
							<th class="px-5 py-3 font-medium">{t('web.templates.col_template')}</th>
						</tr>
					</thead>
					<tbody class="divide-y divide-border">
						{#each data.templates as template (`${template.type}:${template.id}`)}
							{@const key = rowKey(template.type, template.id)}
							<tr>
								<td class="px-5 py-4">
									<input
										type="checkbox"
										class="size-4 rounded border-border-input"
										checked={selected.has(key)}
										disabled={registering}
										aria-label={t('web.templates.select_one', template.name)}
										onchange={event => setSelected(key, event.currentTarget.checked)}
									/>
								</td>
								<td class="px-5 py-4"><MonoId value={template.type} /></td>
								<td class="px-5 py-4">
									<p class="font-medium">{template.name}</p>
									<p><MonoId value={template.id} muted /></p>
								</td>
							</tr>
						{/each}
					</tbody>
				</table>
			</div>

			<div class="border-t border-border p-5">
				<button type="submit" class="btn-primary" disabled={registering || selectedRows.length === 0}>
					{registering ? t('web.templates.registering') : t('web.templates.register')}
				</button>
			</div>
		</section>

		{#if formError}
			<p class="mt-6 text-sm text-destructive" role="alert">{tApiErrorResponse(formError)}</p>
		{/if}
		{#if successMessage}
			<p class="mt-6 text-sm text-foreground">{successMessage}</p>
		{/if}
	</form>
{/if}
