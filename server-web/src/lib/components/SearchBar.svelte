<script lang="ts">
	import { goto } from '$app/navigation';

	let { class: className = '', ...rest } = $props();

	let query = $state('');
	let error = $state('');

	const uuidRe =
		/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

	function handleSubmit(event: SubmitEvent) {
		event.preventDefault();
		error = '';
		const id = query.trim();
		if (!uuidRe.test(id)) {
			error = 'Enter a record UUID';
			return;
		}
		goto(`/records/${id}`);
	}
</script>

<form
	class="flex flex-col gap-1 {className}"
	onsubmit={handleSubmit}
	{...rest}
>
	<div class="flex items-center rounded-2xl border-2 border-pink-950 bg-white">
		<span class="shrink-0 px-3 py-1.5 text-sm font-medium text-zinc-700">Record</span>
		<input
			id="record-search"
			bind:value={query}
			placeholder="Open record by UUID…"
			class="w-full truncate border-x-2 border-y-0 text-base transition-colors sm:text-sm"
			aria-invalid={error ? 'true' : undefined}
		/>
		<button
			type="submit"
			class="shrink-0 px-3 py-1.5 text-sm font-medium text-zinc-800 hover:cursor-pointer"
		>
			Open
		</button>
	</div>
	{#if error}
		<span class="px-2 text-xs text-red-600">{error}</span>
	{/if}
</form>
