<script lang="ts">
	let {
		open = $bindable(false),
		title = 'Audit comment required',
		onconfirm
	}: {
		open?: boolean;
		title?: string;
		onconfirm: (comment: string) => void;
	} = $props();

	let comment = $state('');

	function confirm() {
		const value = comment.trim();
		if (!value) return;
		onconfirm(value);
		comment = '';
		open = false;
	}

	function cancel() {
		comment = '';
		open = false;
	}
</script>

{#if open}
	<div class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
		<div class="w-full max-w-md rounded-lg bg-white p-4 shadow-lg dark:bg-gray-900">
			<h2 class="mb-3 text-lg font-semibold">{title}</h2>
			<label class="flex flex-col gap-1">
				<span class="text-sm">Comment</span>
				<textarea
					bind:value={comment}
					rows="3"
					class="rounded border border-gray-300 px-3 py-2 dark:border-gray-600 dark:bg-gray-950"
				></textarea>
			</label>
			<div class="mt-4 flex justify-end gap-2">
				<button
					type="button"
					class="rounded border border-gray-300 px-3 py-1.5 text-sm"
					onclick={cancel}
				>
					Cancel
				</button>
				<button
					type="button"
					class="rounded bg-(--color-primary) px-3 py-1.5 text-sm text-white disabled:opacity-50"
					disabled={!comment.trim()}
					onclick={confirm}
				>
					Continue
				</button>
			</div>
		</div>
	</div>
{/if}
