<script lang="ts">
	import { resolve } from '$app/paths';
	import { t } from '#lib/i18n/catalog.js';
	import type { StaticRouteId } from '#lib/routing.js';
	import type { Component, Snippet } from 'svelte';
	import type { IconComponentProps } from 'phosphor-svelte';

	let {
		route,
		href,
		label,
		icon,
		active = false,
		class: className = '',
		children,
	}: {
		route?: StaticRouteId;
		href?: string;
		label?: string;
		icon?: Component<IconComponentProps>;
		active?: boolean;
		class?: string;
		children?: Snippet;
	} = $props();

	const linkHref = $derived(href ?? (route ? resolve(route) : undefined));
</script>

{#if linkHref}
	<a href={linkHref} class="header-nav-link {className}" aria-current={active ? 'page' : undefined}>
		{#if icon}
			{@const Icon = icon}
			<Icon class="size-4" aria-hidden="true" />
		{/if}
		{#if children}
			{@render children()}
		{:else if label}
			{t(label)}
		{/if}
	</a>
{/if}

<style>
	@reference "../../../routes/layout.css";

	:global(.header-nav-link) {
		@apply inline-flex h-8 w-max items-center justify-center gap-2 rounded-[7px] bg-transparent px-4 py-2 text-sm font-medium text-primary-foreground outline-hidden transition-colors hover:bg-white/15;

		&:focus-visible {
			@apply bg-white/15 ring-2 ring-primary-foreground ring-inset;
		}

		&[aria-current='page'] {
			@apply bg-white/15;
		}
	}
</style>
