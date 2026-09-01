<script lang="ts">
	import { AuthController } from '$lib/api';
	import type { ActionResponse } from '$lib/api/types.gen';
	import { getApiClient } from '$lib/api-client';
	import PageContent from '$lib/components/layout/PageContent.svelte';
	import UserActionDialog from '$lib/components/UserActionDialog.svelte';
	import { goto } from '$app/navigation';

	let { data } = $props();

	let loggingOut = $state(false);
	let selectedAction = $state<ActionResponse | null>(null);
	let actionOpen = $state(false);

	function openAction(action: ActionResponse) {
		selectedAction = action;
		actionOpen = true;
	}

	function closeActionDialog() {
		actionOpen = false;
		selectedAction = null;
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
								onclick={() => openAction(action)}
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

		<UserActionDialog bind:open={actionOpen} userId={data.me.id} action={selectedAction} onclose={closeActionDialog} />
	{/if}
</PageContent>
