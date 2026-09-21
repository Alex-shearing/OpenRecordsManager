<script lang="ts">
	import { NavigationMenu } from 'bits-ui';
	import SubNavLayout from '$lib/components/layout/SubNavLayout.svelte';
	import SubNavList from '$lib/components/layout/SubNavList.svelte';
	import SubNavLink from '$lib/components/layout/SubNavLink.svelte';
	import SubNavMenu from '$lib/components/layout/SubNavMenu.svelte';
	import SubNavMenuLink from '$lib/components/layout/SubNavMenuLink.svelte';

	let { children } = $props();
</script>

<SubNavLayout>
	{#snippet subnav()}
		<NavigationMenu.Root class="relative">
			<SubNavList>
				<SubNavLink href="/admin/config" match="exact">Configuration</SubNavLink>
				<SubNavLink href="/admin/audit" match="exact">Audit</SubNavLink>
				<SubNavLink href="/admin/auth-providers" match="exact">Login providers</SubNavLink>

				<SubNavMenu label="Manage Plugins" hrefPrefix="/admin/plugins">
					<SubNavMenuLink route="/(authenticated)/admin/plugins">Plugins</SubNavMenuLink>
					<SubNavMenuLink route="/(authenticated)/admin/plugins/templates">Templates</SubNavMenuLink>
				</SubNavMenu>

				<SubNavMenu label="File Stores" hrefPrefix="/admin/file-stores">
					<SubNavMenuLink route="/(authenticated)/admin/file-stores">Stores</SubNavMenuLink>
					<SubNavMenuLink route="/(authenticated)/admin/file-stores/middlewares">Middlewares</SubNavMenuLink>
				</SubNavMenu>
			</SubNavList>
		</NavigationMenu.Root>
	{/snippet}

	{@render children()}
</SubNavLayout>
