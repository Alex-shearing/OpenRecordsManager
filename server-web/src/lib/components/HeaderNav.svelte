<script lang="ts">
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import type { RouteId } from '$app/types';

	type HeaderNavLink = {
		route: RouteId;
		label: string;
	};

	const headerNavLinks: HeaderNavLink[] = [{ route: '/(authenticated)/admin', label: 'Admin' }];
</script>

{#if headerNavLinks.length > 0}
	<nav class="flex shrink-0 items-center gap-1" aria-label="Main">
		{#each headerNavLinks as link (link.route)}
			<a
				href={resolve(link.route)}
				class="header-nav-link"
				aria-current={page.route.id == link.route ? 'page' : undefined}
			>
				{link.label}
			</a>
		{/each}
	</nav>
{/if}
