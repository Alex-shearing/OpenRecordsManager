<script lang="ts">
	import { page } from '$app/state';
	import type { RouteId } from '$app/types';
	import HeaderNavLink from '$lib/components/layout/HeaderNavLink.svelte';
	import GearSixIcon from 'phosphor-svelte/lib/GearSixIcon';
	import { t } from '$lib/i18n/catalog';
	import type { Component } from 'svelte';
	import type { IconComponentProps } from 'phosphor-svelte';

	type HeaderNavLinkConfig = {
		route: RouteId;
		labelKey: `web.nav.${string}`;
		icon: Component<IconComponentProps>;
	};

	const headerNavLinks: HeaderNavLinkConfig[] = [
		{ route: '/(authenticated)/admin', labelKey: 'web.nav.admin', icon: GearSixIcon },
	];
</script>

{#if headerNavLinks.length > 0}
	<nav aria-label={t('web.nav.main')} class="flex shrink-0 items-center gap-1">
		{#each headerNavLinks as link (link.route)}
			<HeaderNavLink route={link.route} label={link.labelKey} icon={link.icon} active={page.route.id === link.route} />
		{/each}
	</nav>
{/if}
