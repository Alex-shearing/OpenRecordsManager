<script lang="ts">
	import { TemplateController, TemplateType } from '#lib/api/index.js';
	import { getApiClient } from '#lib/api-client.js';
	import AdminFormError from '#lib/components/AdminFormError.svelte';
	import AdminPageIntro from '#lib/components/AdminPageIntro.svelte';
	import MonoId from '#lib/components/MonoId.svelte';
	import { type SchemaFormError } from '#lib/components/SchemaForm.svelte';
	import { t } from '#lib/i18n/catalog.js';
	import toast from 'svelte-hot-french-toast';

	let { data } = $props();

	let selected = $state<string[]>([]);
	let registering = $state(false);
	let formError = $state<SchemaFormError>();

	function rowKey(type: string, templateId: string) {
		return `${type}:${templateId}`;
	}

	const selectedRows = $derived(data.templates.filter(row => selected.includes(rowKey(row.type, row.id))));

	const allSelected = $derived(
		data.templates.length > 0 && data.templates.every(row => selected.includes(rowKey(row.type, row.id)))
	);

	function setAllSelected(checked: boolean) {
		if (checked) {
			selected = data.templates.map(row => rowKey(row.type, row.id));
		} else {
			selected = [];
		}
	}

	async function handleRegister(event: SubmitEvent) {
		event.preventDefault();

		if (selectedRows.length === 0) {
			formError = { error: 'web.templates.select_at_least_one' };
			return;
		}

		registering = true;
		formError = undefined;

		const registeredKeys: string[] = [];

		for (const row of selectedRows) {
			const key = rowKey(row.type, row.id);
			const { error } = await TemplateController.registerTemplate({
				client: getApiClient(),
				path: { type: row.type, template: row.id },
				query: { includeDependencies: true },
			});

			if (error) {
				registering = false;
				selected = selected.filter(k => !registeredKeys.includes(k));
				formError = error;
				if (registeredKeys.length > 0) {
					toast.warning(t('web.templates.registered_partial', registeredKeys.length));
				}
				return;
			}

			registeredKeys.push(key);
		}

		selected = selected.filter(key => !registeredKeys.includes(key));
		registering = false;
		toast.success(t('web.templates.registered_count', registeredKeys.length));
	}
</script>

<AdminPageIntro title={t('web.templates.title')} intro={t('web.templates.intro')} error={data.error} />

{#if !data.error}
	<form method="GET" class="mb-4" data-sveltekit-keepfocus data-sveltekit-noscroll data-sveltekit-replacestate>
		<label class="flex flex-col gap-1">
			<span class="text-label">{t('web.templates.filter_type')}</span>
			<select
				name="type"
				class="input max-w-xs"
				value={data.type ?? ''}
				onchange={event => event.currentTarget.form?.requestSubmit()}
			>
				<option value="">{t('web.templates.filter_all')}</option>
				{#each Object.values(TemplateType) as typeValue (typeValue)}
					<option value={typeValue}>{typeValue}</option>
				{/each}
			</select>
		</label>
	</form>

	{#if data.templates.length === 0}
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
											value={key}
											bind:group={selected}
											disabled={registering}
											aria-label={t('web.templates.select_one', template.name)}
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

			<AdminFormError error={formError} class="mt-6" />
		</form>
	{/if}
{/if}
