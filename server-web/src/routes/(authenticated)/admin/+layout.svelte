<script lang="ts">
	import { NavigationMenu } from 'bits-ui';
	import CaretDownIcon from 'phosphor-svelte/lib/CaretDownIcon';

	let { children } = $props();
</script>

<NavigationMenu.Root class="relative z-10 mb-6 flex w-full justify-between bg-background p-2 shadow-sm">
	<NavigationMenu.List class="flex w-full list-none flex-wrap items-center gap-4 sm:gap-8">
		<NavigationMenu.Item value="home">
			<NavigationMenu.Link class="text-foreground hover:text-primary" href="/admin">
				Admin home
			</NavigationMenu.Link>
		</NavigationMenu.Item>
		<NavigationMenu.Item value="config">
			<NavigationMenu.Link class="text-foreground hover:text-primary" href="/admin/config">
				Configuration
			</NavigationMenu.Link>
		</NavigationMenu.Item>
		<NavigationMenu.Item value="plugins">
			<NavigationMenu.Trigger class="inline-flex items-center justify-between">
				Plugins
				<CaretDownIcon
					class="relative top-px ml-1 size-3 transition-transform duration-200 group-data-[state=open]:rotate-180"
					aria-hidden="true"
				/>
			</NavigationMenu.Trigger>
			<NavigationMenu.Content>
				<ul class="grid gap-3 p-3 sm:w-80">
					<NavigationMenu.Link class="text-foreground hover:text-primary" href="/admin/plugins">
						Installed plugins
					</NavigationMenu.Link>
					<NavigationMenu.Link
						class="text-foreground hover:text-primary"
						href="/admin/plugins/templates"
					>
						Templates
					</NavigationMenu.Link>
				</ul>
			</NavigationMenu.Content>
		</NavigationMenu.Item>
		<NavigationMenu.Item value="file-stores">
			<NavigationMenu.Link class="text-foreground hover:text-primary" href="/admin/file-stores">
				File stores
			</NavigationMenu.Link>
		</NavigationMenu.Item>
		<NavigationMenu.Item value="audit">
			<NavigationMenu.Link class="text-foreground hover:text-primary" href="/admin/audit">
				Audit
			</NavigationMenu.Link>
		</NavigationMenu.Item>
		<NavigationMenu.Item value="auth">
			<NavigationMenu.Link class="text-foreground hover:text-primary" href="/admin/auth-providers">
				Auth providers
			</NavigationMenu.Link>
		</NavigationMenu.Item>
	</NavigationMenu.List>

	<div class="perspective-[2000px] absolute left-0 top-full z-200 flex w-full justify-center">
		<NavigationMenu.Viewport
			class="text-popover-foreground bg-background data-[state=closed]:animate-scale-out data-[state=open]:animate-scale-in relative mt-2.5 h-(--bits-navigation-menu-viewport-height) w-full origin-[top_center] overflow-hidden rounded-md border shadow-lg transition-[width,height] duration-200 sm:w-(--bits-navigation-menu-viewport-width)"
		/>
	</div>
</NavigationMenu.Root>

{@render children()}
