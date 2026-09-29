<script lang="ts">
	import { NavigationMenu } from 'bits-ui';
	import SubNavLayout from '$lib/components/layout/SubNavLayout.svelte';
	import SubNavList from '$lib/components/layout/SubNavList.svelte';
	import SubNavLink from '$lib/components/layout/SubNavLink.svelte';
	import SubNavMenu from '$lib/components/layout/SubNavMenu.svelte';
	import SubNavMenuLink from '$lib/components/layout/SubNavMenuLink.svelte';
	import { t } from '$lib/i18n/catalog';

	let { children } = $props();
</script>

<SubNavLayout>
	{#snippet subnav()}
		<NavigationMenu.Root class="relative">
			<SubNavList>
				<SubNavLink href="/admin/config" match="exact">{t('web.admin.configuration')}</SubNavLink>
				<SubNavLink href="/admin/audit" match="exact">{t('web.admin.audit')}</SubNavLink>
				<SubNavLink href="/admin/auth-providers" match="exact">{t('web.admin.login_providers')}</SubNavLink>

				<SubNavMenu label={t('web.admin.manage_plugins')} hrefPrefix="/admin/plugins">
					<SubNavMenuLink route="/(authenticated)/admin/plugins">{t('web.admin.plugins')}</SubNavMenuLink>
					<SubNavMenuLink route="/(authenticated)/admin/plugins/templates">{t('web.admin.templates')}</SubNavMenuLink>
				</SubNavMenu>

				<SubNavMenu label={t('web.admin.file_stores')} hrefPrefix="/admin/file-stores">
					<SubNavMenuLink route="/(authenticated)/admin/file-stores">{t('web.admin.stores')}</SubNavMenuLink>
					<SubNavMenuLink route="/(authenticated)/admin/file-stores/middlewares">
						{t('web.admin.middlewares')}
					</SubNavMenuLink>
				</SubNavMenu>
			</SubNavList>
		</NavigationMenu.Root>
	{/snippet}

	{@render children()}
</SubNavLayout>
