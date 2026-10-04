<script lang="ts">
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import HeaderNavLink from '$lib/components/layout/HeaderNavLink.svelte';
	import { t } from '$lib/i18n/catalog';
	import { NavigationMenu } from 'bits-ui';
	import CaretDownIcon from 'phosphor-svelte/lib/CaretDownIcon';
	import FilePlusIcon from 'phosphor-svelte/lib/FilePlusIcon';
	import GearSixIcon from 'phosphor-svelte/lib/GearSixIcon';
	import PlusIcon from 'phosphor-svelte/lib/PlusIcon';
	import UsersThreeIcon from 'phosphor-svelte/lib/UsersThreeIcon';

	const newRecordRoute = '/(authenticated)/records/edit/new';
	const newLocationRoute = '/(authenticated)/locations/edit/new';

	const isNewMenuActive = $derived(page.route.id === newRecordRoute || page.route.id === newLocationRoute);
	const isNewRecordActive = $derived(page.route.id === newRecordRoute);
	const isNewLocationActive = $derived(page.route.id === newLocationRoute);
	const isAdminActive = $derived(page.route.id === '/(authenticated)/admin');
</script>

<nav aria-label={t('web.nav.main')} class="flex shrink-0 items-center gap-1">
	<NavigationMenu.Root class="relative">
		<NavigationMenu.List class="flex items-center gap-1">
			<NavigationMenu.Item class="relative">
				<NavigationMenu.Trigger
					class="header-nav-link group data-active:bg-white/15 data-[state=open]:bg-white/15"
					data-active={isNewMenuActive ? '' : undefined}
				>
					<PlusIcon class="size-4" aria-hidden="true" />
					{t('web.nav.new')}
					<CaretDownIcon
						class="size-3.5 transition-transform duration-200 group-data-[state=open]:rotate-180"
						aria-hidden="true"
					/>
				</NavigationMenu.Trigger>
				<NavigationMenu.Content
					class="absolute top-full left-0 z-20 mt-1 rounded-lg border border-border bg-surface text-foreground shadow-lg"
				>
					<ul class="grid min-w-44 gap-1 p-2">
						<li>
							<NavigationMenu.Link
								href={resolve(newRecordRoute)}
								class="flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-surface-hover hover:text-foreground aria-[current=page]:bg-primary/8 aria-[current=page]:text-primary"
								aria-current={isNewRecordActive ? 'page' : undefined}
							>
								<FilePlusIcon class="size-4" aria-hidden="true" />
								{t('web.nav.new_record')}
							</NavigationMenu.Link>
						</li>
						<li>
							<NavigationMenu.Link
								href={resolve(newLocationRoute)}
								class="flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-surface-hover hover:text-foreground aria-[current=page]:bg-primary/8 aria-[current=page]:text-primary"
								aria-current={isNewLocationActive ? 'page' : undefined}
							>
								<UsersThreeIcon class="size-4" aria-hidden="true" />
								{t('web.nav.new_location')}
							</NavigationMenu.Link>
						</li>
					</ul>
				</NavigationMenu.Content>
			</NavigationMenu.Item>
		</NavigationMenu.List>
	</NavigationMenu.Root>

	<HeaderNavLink route="/(authenticated)/admin" label="web.nav.admin" icon={GearSixIcon} active={isAdminActive} />
</nav>
