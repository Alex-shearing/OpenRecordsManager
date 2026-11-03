<script lang="ts">
	import type { ManagedObjectTypePropertyResponse, SimpleObjectPropertyResponse } from '#lib/api/types.gen.js';
	import { t } from '#lib/i18n/catalog.js';
	import { objectPropertyName } from '#lib/i18n/labels.js';

	let {
		properties,
		selectedIds = $bindable<string[]>([]),
		defaults = $bindable<Record<string, string>>({}),
		inheritedProperties = [],
		disabled = false,
	}: {
		properties: SimpleObjectPropertyResponse[];
		selectedIds?: string[];
		defaults?: Record<string, string>;
		inheritedProperties?: ManagedObjectTypePropertyResponse[];
		disabled?: boolean;
	} = $props();

	const inheritedById = $derived(new Map(inheritedProperties.map(entry => [entry.property.id, entry])));
	const inheritedIds = $derived(new Set(inheritedById.keys()));

	const sortedProperties = $derived(
		[...properties].sort((a, b) => objectPropertyName(a.id).localeCompare(objectPropertyName(b.id)))
	);

	const fieldId = $props.id();

	function inheritedDefaultText(propertyId: string): string {
		const value = inheritedById.get(propertyId)?.default;
		if (value === undefined || value === null) {
			return '';
		}
		return typeof value === 'string' ? value : JSON.stringify(value);
	}
</script>

<fieldset class="flex flex-col gap-3" {disabled}>
	<legend class="text-label">{t('web.types.properties')}</legend>
	<p id="{fieldId}-hint" class="text-hint text-sm">{t('web.types.properties_hint')}</p>

	{#if sortedProperties.length === 0}
		<p class="text-hint">{t('web.types.no_properties')}</p>
	{:else}
		<ul class="divide-y divide-border rounded-md border border-border" aria-describedby="{fieldId}-hint">
			{#each sortedProperties as property (property.id)}
				{@const inherited = inheritedIds.has(property.id)}
				{@const checked = inherited || selectedIds.includes(property.id)}
				{@const defaultId = `${fieldId}-${property.id}-default`}
				<li
					class="flex flex-col gap-2 p-3 sm:flex-row sm:items-start {inherited
						? 'bg-surface-muted/60 text-muted-foreground'
						: ''}"
				>
					<label class="flex min-w-0 flex-1 items-start gap-2 {inherited ? 'cursor-not-allowed' : ''}">
						{#if inherited}
							<input type="checkbox" class="mt-1" checked disabled />
						{:else}
							<!-- Orphan defaults are retained so re-checking a property restores its prior default. -->
							<input type="checkbox" class="mt-1" value={property.id} bind:group={selectedIds} />
						{/if}
						<span class="min-w-0">
							<span class="block font-medium">{objectPropertyName(property.id)}</span>
							<span class="text-hint font-mono text-xs">{property.id}</span>
							{#if inherited}
								<span class="text-hint mt-1 block text-xs">{t('web.types.property_inherited')}</span>
							{/if}
						</span>
					</label>
					{#if checked}
						<label class="flex w-full flex-col gap-1 sm:max-w-xs" for={defaultId}>
							<span class="text-label text-xs">{t('web.types.property_default')}</span>
							{#if inherited}
								<input
									id={defaultId}
									class="input w-full font-mono text-sm"
									value={inheritedDefaultText(property.id)}
									placeholder={t('web.types.property_default_placeholder')}
									readonly
									disabled
								/>
							{:else}
								<input
									id={defaultId}
									class="input w-full font-mono text-sm"
									name={`property-default-${property.id}`}
									placeholder={t('web.types.property_default_placeholder')}
									bind:value={
										() => defaults[property.id] ?? '',
										v => {
											defaults = { ...defaults, [property.id]: v };
										}
									}
								/>
							{/if}
						</label>
					{/if}
				</li>
			{/each}
		</ul>
	{/if}
</fieldset>
