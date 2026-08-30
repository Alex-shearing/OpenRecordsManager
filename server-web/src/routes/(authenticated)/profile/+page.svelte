<script lang="ts">
	import { AuthController } from '$lib';
	import UserActionForm from '$lib/components/UserActionForm.svelte';
	import { goto } from '$app/navigation';

	let { data } = $props();
	let loggingOut = $state(false);

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
					<dt class="text-sm text-gray-500">{data.properties.find((a) => a.id === key)?.name || key}</dt>
					<dd>{String(value)}</dd>
				</div>
			{/each}
		</dl>
	</section>

	{#if data.actions.length > 0}
		<section class="mb-8">
			<h2 class="mb-4 text-lg font-medium">Actions</h2>
			<div class="flex flex-col gap-4">
				{#each data.actions as action (action.id)}
					<UserActionForm userId={data.me.id} {action} />
				{/each}
			</div>
		</section>
	{/if}

	<section class="rounded-lg border border-gray-200 p-4 dark:border-gray-700">
		<h2 class="text-lg font-medium">Sign out</h2>
		<p class="mt-1 text-sm text-gray-600 dark:text-gray-300">
			End your current session on this device.
		</p>
		<button
			type="button"
			class="mt-4 rounded border border-gray-300 px-4 py-2 hover:bg-gray-50 disabled:opacity-50 dark:border-gray-600 dark:hover:bg-gray-900"
			disabled={loggingOut}
			onclick={handleLogout}
		>
			{loggingOut ? 'Signing out...' : 'Sign out'}
		</button>
	</section>
{/if}
