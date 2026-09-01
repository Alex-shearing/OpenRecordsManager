<script lang="ts">
	import { NavigationMenu } from 'bits-ui';
	import CaretDownIcon from 'phosphor-svelte/lib/CaretDownIcon';
	import { page } from '$app/state';
	import type { Snippet } from 'svelte';

	let {
		label,
		hrefPrefix,
		children,
	}: {
		label: string;
		hrefPrefix: string;
		children: Snippet;
	} = $props();

	const isActive = $derived(page.url.pathname === hrefPrefix || page.url.pathname.startsWith(`${hrefPrefix}/`));
</script>

<NavigationMenu.Item>
	<NavigationMenu.Trigger
		class="group inline-flex items-center gap-1 rounded-md px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-surface-hover hover:text-foreground data-[state=open]:bg-surface-hover data-active:bg-primary/8 data-active:text-primary"
		data-active={isActive ? '' : undefined}
	>
		{label}
		<CaretDownIcon
			class="size-3.5 transition-transform duration-200 group-data-[state=open]:rotate-180"
			aria-hidden="true"
		/>
	</NavigationMenu.Trigger>
	<NavigationMenu.Content>
		<ul class="grid min-w-48 gap-1 p-2">
			{@render children()}
		</ul>
	</NavigationMenu.Content>
</NavigationMenu.Item>
