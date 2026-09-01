<script lang="ts">
	import { NavigationMenu } from 'bits-ui';
	import type { Snippet } from 'svelte';

	let { children }: { children: Snippet } = $props();
</script>

<NavigationMenu.List class="flex list-none items-center gap-1">
	{@render children()}
</NavigationMenu.List>
