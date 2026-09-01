<script lang="ts">
	import { page } from '$app/state';
	import type { RouteId } from '$app/types';
	import HeaderNavLink from '$lib/components/layout/HeaderNavLink.svelte';
	import GearSixIcon from 'phosphor-svelte/lib/GearSixIcon';
	import type { Component } from 'svelte';
	import type { IconComponentProps } from 'phosphor-svelte';

	type HeaderNavLinkConfig = {
		route: RouteId;
		label: string;
		icon: Component<IconComponentProps>;
	};

	const headerNavLinks: HeaderNavLinkConfig[] = [
		{ route: '/(authenticated)/admin', label: 'Admin', icon: GearSixIcon },
	];
</script>

{#if headerNavLinks.length > 0}
	<nav aria-label="Main" class="flex shrink-0 items-center gap-1">
		{#each headerNavLinks as link (link.route)}
			<HeaderNavLink route={link.route} label={link.label} icon={link.icon} active={page.route.id === link.route} />
		{/each}
	</nav>
{/if}
