<script lang="ts">
	const links = [
		{ href: '/admin/config', title: 'Configuration', desc: 'Database and server config values' },
		{ href: '/admin/plugins', title: 'Plugins', desc: 'Enabled plugin names' },
		{ href: '/admin/plugins/templates', title: 'Templates', desc: 'Browse and register templates' },
		{ href: '/admin/file-stores', title: 'File stores', desc: 'Stores and middlewares' },
		{ href: '/admin/audit', title: 'Audit', desc: 'Status, policies, and events' },
		{ href: '/admin/auth-providers', title: 'Auth providers', desc: 'List and create providers' }
	];
</script>

<div class="flex flex-col gap-4">
	<h1 class="text-2xl font-semibold">Administration</h1>
	<ul class="grid gap-3 sm:grid-cols-2">
		{#each links as link (link.href)}
			<li>
				<a
					href={link.href}
					class="block rounded-lg border border-gray-200 p-4 hover:border-(--color-primary) dark:border-gray-700"
				>
					<h2 class="font-medium">{link.title}</h2>
					<p class="text-sm text-gray-500">{link.desc}</p>
				</a>
			</li>
		{/each}
	</ul>
</div>
