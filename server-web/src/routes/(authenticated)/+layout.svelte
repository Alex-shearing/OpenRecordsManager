<script lang="ts">
	import { NavigationMenu } from 'bits-ui';
	import SearchBar from '$lib/components/SearchBar.svelte';
	import { getClient } from '$lib';
	import { clearAuthCookie } from '$lib/api';
	import { config } from '$lib/config.svelte';
	import { goto } from '$app/navigation';
	import type { components } from '$lib/types/schema';

	type UserResponse = components['schemas']['UserResponse'];

	let { children } = $props();

	const branding = config.getConfig();
	let user = $state<UserResponse | null>(null);
	let userLoaded = $state(false);

	function unwrapMe(data: unknown): UserResponse | null {
		if (!data || typeof data !== 'object') return null;
		const record = data as Record<string, unknown>;
		if (record.data && typeof record.data === 'object' && record.data !== null && 'username' in record.data) {
			return record.data as UserResponse;
		}
		if ('username' in record && 'id' in record) return record as UserResponse;
		return null;
	}

	$effect(() => {
		getClient()
			.GET('/api/user/me')
			.then(({ data }) => {
				user = unwrapMe(data);
				userLoaded = true;
			})
			.catch(() => {
				user = null;
				userLoaded = true;
			});
	});

	function logout() {
		clearAuthCookie();
		user = null;
		goto('/login');
	}
</script>

<NavigationMenu.Root
	class="relative z-15 flex w-full flex-wrap items-center justify-between gap-3 bg-pink-300 p-2 shadow"
>
	<div class="flex items-center gap-4">
		<a href="/" class="text-xl font-bold">
			{#if branding.logoUrl}
				<img src={branding.logoUrl} alt={branding.productName} class="h-8" />
			{:else}
				{branding.productName}
			{/if}
		</a>
		<nav class="hidden items-center gap-3 text-sm font-medium sm:flex">
			<a href="/records/new" class="hover:underline">New record</a>
			<a href="/record-types" class="hover:underline">Types</a>
			<a href="/lists" class="hover:underline">Lists</a>
			<a href="/object-properties" class="hover:underline">Properties</a>
			<a href="/admin" class="hover:underline">Admin</a>
		</nav>
	</div>

	<SearchBar class="w-full sm:w-1/2" />

	<div class="flex items-center gap-2">
		{#if userLoaded && user}
			<a
				href="/profile"
				class="hidden rounded px-3 py-1.5 text-sm font-medium hover:bg-white sm:inline-flex"
			>
				{user.username}
			</a>
			<button
				type="button"
				class="rounded px-3 py-1.5 text-sm font-medium hover:bg-white"
				onclick={logout}
			>
				Sign out
			</button>
		{:else if userLoaded}
			<a
				href="/login"
				class="rounded px-3 py-1.5 text-sm font-medium hover:bg-white"
			>
				Sign in
			</a>
		{/if}
	</div>
</NavigationMenu.Root>

<main class="mx-auto max-w-5xl px-4 py-6">
	{@render children()}
</main>
