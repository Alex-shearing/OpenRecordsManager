<script lang="ts">
	import { goto } from '$app/navigation';

	let recordId = $state('');
	let error = $state('');

	const uuidRe =
		/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

	function openRecord(event: SubmitEvent) {
		event.preventDefault();
		error = '';
		const id = recordId.trim();
		if (!uuidRe.test(id)) {
			error = 'Enter a valid record UUID';
			return;
		}
		goto(`/records/${id}`);
	}
</script>

<div class="flex flex-col gap-8">
	<section>
		<h1 class="mb-2 text-2xl font-semibold">Open Records Manager</h1>
		<p class="text-sm text-gray-600">
			Open a record by ID, create a new record, or manage lists and configuration.
		</p>
	</section>

	<section class="rounded-lg border border-gray-200 p-4 dark:border-gray-700">
		<h2 class="mb-3 text-lg font-medium">Open record</h2>
		<form class="flex flex-col gap-2 sm:flex-row sm:items-start" onsubmit={openRecord}>
			<input
				type="text"
				bind:value={recordId}
				placeholder="Record UUID"
				class="flex-1 rounded border border-gray-300 px-3 py-2 dark:border-gray-600"
			/>
			<button
				type="submit"
				class="rounded bg-(--color-primary) px-4 py-2 text-sm font-medium text-white"
			>
				Open
			</button>
		</form>
		{#if error}
			<p class="mt-2 text-sm text-red-600">{error}</p>
		{/if}
	</section>

	<section class="grid gap-3 sm:grid-cols-2">
		<a
			href="/records/new"
			class="rounded-lg border border-gray-200 p-4 hover:border-(--color-primary) dark:border-gray-700"
		>
			<h3 class="font-medium">Create record</h3>
			<p class="text-sm text-gray-500">Choose a type and set properties</p>
		</a>
		<a
			href="/record-types"
			class="rounded-lg border border-gray-200 p-4 hover:border-(--color-primary) dark:border-gray-700"
		>
			<h3 class="font-medium">Record types</h3>
			<p class="text-sm text-gray-500">Browse registered types</p>
		</a>
		<a
			href="/lists"
			class="rounded-lg border border-gray-200 p-4 hover:border-(--color-primary) dark:border-gray-700"
		>
			<h3 class="font-medium">Lists</h3>
			<p class="text-sm text-gray-500">Manage list types and elements</p>
		</a>
		<a
			href="/object-properties"
			class="rounded-lg border border-gray-200 p-4 hover:border-(--color-primary) dark:border-gray-700"
		>
			<h3 class="font-medium">Object properties</h3>
			<p class="text-sm text-gray-500">Define metadata fields</p>
		</a>
		<a
			href="/admin"
			class="rounded-lg border border-gray-200 p-4 hover:border-(--color-primary) dark:border-gray-700"
		>
			<h3 class="font-medium">Administration</h3>
			<p class="text-sm text-gray-500">Config, plugins, file stores, audit</p>
		</a>
		<a
			href="/profile"
			class="rounded-lg border border-gray-200 p-4 hover:border-(--color-primary) dark:border-gray-700"
		>
			<h3 class="font-medium">Profile</h3>
			<p class="text-sm text-gray-500">Your account and user actions</p>
		</a>
	</section>
</div>
