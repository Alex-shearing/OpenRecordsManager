<script lang="ts">
	import { AuthController } from '$lib/api';
	import type { ActionResponse } from '$lib/api/types.gen';
	import { getApiClient } from '$lib/api-client';
	import PageContent from '$lib/components/layout/PageContent.svelte';
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
		await AuthController.logout({ client: getApiClient() });
		await goto('/login');
	}
</script>

<PageContent>
	<h1 class="mb-6 text-2xl font-semibold">Profile</h1>

	{#if data.error}
		<p class="text-destructive">{data.error}</p>
	{:else if data.me}
		<section class="card mb-8 p-4">
			<h2 class="text-lg font-medium">Account</h2>
			<dl class="mt-4 grid gap-3 sm:grid-cols-2">
				<div>
					<dt class="text-hint">Username</dt>
					<dd class="font-medium">{data.me.username}</dd>
				</div>
				<div>
					<dt class="text-hint">User ID</dt>
					<dd class="font-mono text-sm">{data.me.id}</dd>
				</div>
				{#each Object.entries(data.me.properties) as [key, value] (key)}
					<div>
						<dt class="text-hint">
							{data.properties.find(property => property.id === key)?.name || key}
						</dt>
						<dd>{String(value)}</dd>
					</div>
				{/each}
			</dl>
		</section>

		{#if data.actions.length > 0}
			<section class="mb-8">
				<h2 class="mb-4 text-lg font-medium">Actions</h2>
				<ul class="list-panel">
					{#each data.actions as action (action.id)}
						<li>
							<button
								type="button"
								class="list-panel-item flex w-full flex-col gap-1 text-left"
								onclick={() => (selectedAction = action)}
								commandfor="action-modal"
								command="show-modal"
							>
								<span class="font-medium">{action.name}</span>
								{#if action.description}
									<span class="text-hint">{action.description}</span>
								{/if}
							</button>
						</li>
					{/each}
				</ul>
			</section>
		{/if}

		<section class="card p-4">
			<h2 class="text-lg font-medium">Sign out</h2>
			<p class="mt-1 text-hint">End your current session on this device.</p>
			<button type="button" class="btn-secondary mt-4" disabled={loggingOut} onclick={handleLogout}>
				{loggingOut ? 'Signing out...' : 'Sign out'}
			</button>
		</section>

		<dialog bind:this={dialog} class="action-dialog" id="action-modal">
			{#if selectedAction}
				{#key selectedAction.id}
					<div class="card-header flex items-start justify-between gap-4">
						<div>
							<h2 class="text-lg font-medium">{selectedAction.name}</h2>
							{#if selectedAction.description}
								<p class="mt-1 text-hint">{selectedAction.description}</p>
							{/if}
						</div>
						<button
							type="button"
							aria-label="Close"
							commandfor="action-modal"
							command="close"
							class="btn-ghost px-2 py-0.5 text-muted-foreground"
						>
							×
						</button>
					</div>

					<div class="card-body">
						<UserActionForm userId={data.me.id} action={selectedAction} onsuccess={closeDialog} />
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
			border: 1px solid var(--color-border);
			border-radius: var(--radius-lg);
			background: var(--color-surface);
			color: var(--color-foreground);
			box-shadow: var(--shadow-dialog);
			opacity: 0;
			transform: scale(0.97);
			transition:
				opacity 160ms ease-out,
				transform 160ms ease-out,
				overlay 160ms allow-discrete,
				display 160ms allow-discrete;
		}

		.action-dialog[open] {
			opacity: 1;
			transform: scale(1);
		}

		@starting-style {
			.action-dialog[open] {
				opacity: 0;
				transform: scale(0.97);
			}
		}

		.action-dialog::backdrop {
			background: rgb(0 0 0 / 0.25);
			opacity: 0;
			transition:
				opacity 160ms ease-out,
				overlay 160ms allow-discrete,
				display 160ms allow-discrete;
		}

		.action-dialog[open]::backdrop {
			opacity: 1;
		}

		@starting-style {
			.action-dialog[open]::backdrop {
				opacity: 0;
			}
		}

		@media (prefers-reduced-motion: reduce) {
			.action-dialog,
			.action-dialog::backdrop {
				transition: none;
				transform: none;
			}
		}
	</style>
</PageContent>
