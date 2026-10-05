<script lang="ts">
	import { resolve } from '$app/paths';
	import ActionList from '#lib/components/ActionList.svelte';
	import MonoId from '#lib/components/MonoId.svelte';
	import PageContent from '#lib/components/layout/PageContent.svelte';
	import PropertyDisplayCard from '#lib/components/PropertyDisplayCard.svelte';
	import RecordRevisionsCard from '#lib/components/RecordRevisionsCard.svelte';
	import { t, tApiErrorResponse } from '#lib/i18n/catalog.js';
	import { recordTypeName } from '#lib/i18n/labels.js';

	let { data } = $props();

	const pageTitle = $derived.by(() => {
		const record = data.record;
		if (!record) {
			return t('web.records.view_title');
		}
		const title = record.properties['builtin:title'];
		if (typeof title === 'string' && title.trim()) {
			return title;
		}
		return recordTypeName(record.type);
	});
</script>

<PageContent>
	{#if data.loadError}
		<p class="text-destructive">{tApiErrorResponse(data.loadError)}</p>
	{:else if !data.record}
		<p class="text-destructive">{t('web.records.not_found')}</p>
	{:else}
		{@const record = data.record}
		<div class="mb-6 flex flex-wrap items-start justify-between gap-4">
			<h1 class="text-2xl font-semibold">{pageTitle}</h1>
			<a href={resolve('/(authenticated)/records/edit/[id]', { id: record.id })} class="btn-secondary">
				{t('web.common.edit')}
			</a>
		</div>

		<PropertyDisplayCard properties={record.properties} definitions={data.properties}>
			{#snippet before()}
				<div>
					<dt class="text-hint">{t('web.records.id')}</dt>
					<dd><MonoId value={record.id} /></dd>
				</div>
				<div>
					<dt class="text-hint">{t('web.common.type')}</dt>
					<dd>{recordTypeName(record.type)}</dd>
				</div>
			{/snippet}
		</PropertyDisplayCard>

		<RecordRevisionsCard
			{record}
			types={data.types}
			auditCommentRequired={data.auditCommentRequired.createRevision}
		/>

		<ActionList actions={data.actions} kind="record" targetId={record.id} />
	{/if}
</PageContent>
