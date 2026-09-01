<script lang="ts">
	import { TemplateController } from '$lib/api';
	import { getApiClient } from '$lib/api-client';

	let { data } = $props();

	const templateRows = $derived(
		data.sections
			.flatMap(section =>
				section.error
					? []
					: section.templates.map(template => ({
							type: section.type,
							id: template.id,
							name: template.name,
						}))
			)
			.sort((a, b) => a.type.localeCompare(b.type) || a.name.localeCompare(b.name))
	);

	const sectionErrors = $derived(data.sections.filter(section => section.error));

	let selected = $state<Set<string>>(new Set());
	let registering = $state(false);
	let formError = $state('');
	let successMessage = $state('');

	function rowKey(type: string, templateId: string) {
		return `${type}:${templateId}`;
	}

	const selectedRows = $derived(templateRows.filter(row => selected.has(rowKey(row.type, row.id))));

	const allSelected = $derived(
		templateRows.length > 0 && templateRows.every(row => selected.has(rowKey(row.type, row.id)))
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
		selected = checked ? new Set(templateRows.map(row => rowKey(row.type, row.id))) : new Set();
	}

	async function handleRegister(event: SubmitEvent) {
		event.preventDefault();

		if (selectedRows.length === 0) {
			formError = 'Select at least one template to register.';
			return;
		}

		registering = true;
		formError = '';
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
				formError = error.error ?? `Failed to register ${row.name}.`;
				if (registeredKeys.size > 0) {
					successMessage = `Registered ${registeredKeys.size} template(s) before the error.`;
				}
				return;
			}

			registeredKeys.add(key);
		}

		selected = new Set([...selected].filter(key => !registeredKeys.has(key)));
		registering = false;
		successMessage = `Registered ${registeredKeys.size} template(s).`;
	}
</script>

<h1 class="mb-2 text-2xl font-semibold">Templates</h1>
<p class="mb-6 text-hint">
	Register plugin-provided templates into the database. Dependencies are included automatically when registering.
</p>

{#if data.error}
	<p class="text-destructive">{data.error}</p>
{:else if data.sections.length === 0}
	<p class="text-hint">No template types are available.</p>
{:else}
	{#each sectionErrors as section (section.type)}
		<p class="mb-4 text-sm text-destructive">Failed to load {section.type}: {section.error}</p>
	{/each}

	{#if templateRows.length === 0}
		<p class="text-hint">No templates are available.</p>
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
										aria-label="Select all templates"
										onchange={event => setAllSelected(event.currentTarget.checked)}
									/>
								</th>
								<th class="px-5 py-3 font-medium">Type</th>
								<th class="px-5 py-3 font-medium">Template</th>
							</tr>
						</thead>
						<tbody class="divide-y divide-border">
							{#each templateRows as template (`${template.type}:${template.id}`)}
								{@const key = rowKey(template.type, template.id)}
								<tr>
									<td class="px-5 py-4">
										<input
											type="checkbox"
											class="size-4 rounded border-border-input"
											checked={selected.has(key)}
											disabled={registering}
											aria-label="Select {template.name}"
											onchange={event => setSelected(key, event.currentTarget.checked)}
										/>
									</td>
									<td class="px-5 py-4 font-mono font-medium">{template.type}</td>
									<td class="px-5 py-4">
										<p class="font-medium">{template.name}</p>
										<p class="font-mono text-hint">{template.id}</p>
									</td>
								</tr>
							{/each}
						</tbody>
					</table>
				</div>

				<div class="border-t border-border p-5">
					<button type="submit" class="btn-primary" disabled={registering || selectedRows.length === 0}>
						{registering ? 'Registering…' : 'Register selected'}
					</button>
				</div>
			</section>

			{#if formError}
				<p class="mt-6 text-sm text-destructive" role="alert">{formError}</p>
			{/if}
			{#if successMessage}
				<p class="mt-6 text-sm text-foreground">{successMessage}</p>
			{/if}
		</form>
	{/if}
{/if}
