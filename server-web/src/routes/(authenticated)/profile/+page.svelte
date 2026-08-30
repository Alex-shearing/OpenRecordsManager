<script lang="ts">
	import { AuthController } from '$lib';
	import type { ActionResponse } from '$lib/api/types.gen';
	import UserActionForm from '$lib/components/UserActionForm.svelte';
	import { goto } from '$app/navigation';

	let { data } = $props();

	let loggingOut = $state(false);
	let selectedAction = $state<ActionResponse | null>(null);
	let dialog = $state<HTMLDialogElement | null>(null);

	function closeDialog() {
		if (dialog?.open) {
			dialog.close();
		}
	}

	async function handleLogout() {
		loggingOut = true;
		await AuthController.logout();
		await goto('/login');
	}
</script>

<h1 class="mb-6 text-2xl font-semibold">Profile</h1>

{#if data.error}
	<p class="text-red-600">{data.error}</p>
{:else if data.me}
	<section class="mb-8 rounded-lg border border-gray-200 p-4 dark:border-gray-700">
		<h2 class="text-lg font-medium">Account</h2>
		<dl class="mt-4 grid gap-3 sm:grid-cols-2">
			<div>
				<dt class="text-sm text-gray-500">Username</dt>
				<dd class="font-medium">{data.me.username}</dd>
			</div>
			<div>
				<dt class="text-sm text-gray-500">User ID</dt>
				<dd class="font-mono text-sm">{data.me.id}</dd>
			</div>
			{#each Object.entries(data.me.properties) as [key, value] (key)}
				<div>
					<dt class="text-sm text-gray-500">
						{data.properties.find((property) => property.id === key)?.name || key}
					</dt>
					<dd>{String(value)}</dd>
				</div>
			{/each}
		</dl>
	</section>

	{#if data.actions.length > 0}
		<section class="mb-8">
			<h2 class="mb-4 text-lg font-medium">Actions</h2>
			<ul class="divide-y divide-gray-200 overflow-hidden rounded-lg border border-gray-200 dark:divide-gray-700 dark:border-gray-700">
				{#each data.actions as action (action.id)}
					<li>
						<button
							type="button"
							class="flex w-full flex-col gap-1 px-4 py-3 text-left transition-colors hover:bg-gray-100"
							onclick={() => selectedAction = action}
							commandfor="action-modal"
							command="show-modal"
						>
							<span class="font-medium">{action.name}</span>
							{#if action.description}
								<span class="text-sm text-gray-600 dark:text-gray-300">{action.description}</span>
							{/if}
						</button>
					</li>
				{/each}
			</ul>
		</section>
	{/if}

	<section class="rounded-lg border border-gray-200 p-4 dark:border-gray-700">
		<h2 class="text-lg font-medium">Sign out</h2>
		<p class="mt-1 text-sm text-gray-600 dark:text-gray-300">
			End your current session on this device.
		</p>
		<button
			type="button"
			class="mt-4 rounded border border-gray-300 px-4 py-2 hover:bg-gray-100 disabled:opacity-50"
			disabled={loggingOut}
			onclick={handleLogout}
		>
			{loggingOut ? 'Signing out...' : 'Sign out'}
		</button>
	</section>

	<dialog
		bind:this={dialog}
		class="action-dialog"
		id="action-modal"
	>
		{#if selectedAction}
			{#key selectedAction.id}
				<div class="flex items-start justify-between gap-4 border-b border-gray-200 p-4 px-5">
					<div>
						<h2 class="text-lg font-medium">{selectedAction.name}</h2>
						{#if selectedAction.description}
							<p class="mt-1 text-sm text-gray-600">{selectedAction.description}</p>
						{/if}
					</div>
					<button
						type="button"
						aria-label="Close"
						commandfor="action-modal"
						command="close"
						class="py-0.5 px-2 text-gray-600 hover:bg-gray-100"
					>
						×
					</button>
				</div>

				<div class="py-4 px-6">
					<UserActionForm
						userId={data.me.id}
						action={selectedAction}
						onsuccess={closeDialog}
					/>
				</div>
			{/key}
		{/if}
	</dialog>
{/if}

<style>
	.action-dialog {
		position: fixed;
		inset: 0;
		width: fit-content;
		height: fit-content;
		max-width: min(32rem, calc(100vw - 2rem));
		max-height: calc(100vh - 2rem);
		margin: auto;
		padding: 0;
		overflow: auto;
		border: 1px solid #e5e7eb;
		border-radius: 0.5rem;
		background: #fff;
		color: #111827;
		box-shadow:
			0 20px 25px -5px rgb(0 0 0 / 0.1),
			0 8px 10px -6px rgb(0 0 0 / 0.1);
	}

	.action-dialog::backdrop {
		background: rgb(0 0 0 / 0.25);
	}
</style>
