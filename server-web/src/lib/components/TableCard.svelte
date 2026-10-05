<script lang="ts" generics="T">
	import type { Attachment } from 'svelte/attachments';
	import type { Snippet } from 'svelte';
	import type { ClassValue, HTMLAttributes } from 'svelte/elements';

	type Props = Omit<HTMLAttributes<HTMLElement>, 'children'> & {
		title: string;
		items: T[];
		empty: string;
		getKey: (item: T) => string | number;
		getHref?: (item: T) => string;
		/** Optional localStorage key for column widths. Falls back to `title`. */
		storageKey?: string;
		header: Snippet;
		row: Snippet<[T]>;
		actions?: Snippet;
		overlay?: Snippet;
		class?: ClassValue;
	};

	let {
		title,
		items,
		empty,
		getKey,
		getHref,
		storageKey,
		header,
		row,
		actions,
		overlay,
		class: className = '',
		...rest
	}: Props = $props();

	const MIN_COL_WIDTH = 80;
	const STORAGE_PREFIX = 'orm.table-card-cols:';

	function contentHeaderCells(table: HTMLTableElement): HTMLTableCellElement[] {
		return [...table.querySelectorAll<HTMLTableCellElement>('thead th')].filter(
			th => th.getAttribute('aria-hidden') !== 'true'
		);
	}

	function readStoredWidths(key: string): number[] | null {
		try {
			const raw = localStorage.getItem(STORAGE_PREFIX + key);
			if (!raw) return null;
			const parsed = JSON.parse(raw) as unknown;
			if (!Array.isArray(parsed) || !parsed.every(n => typeof n === 'number')) return null;
			return parsed;
		} catch {
			return null;
		}
	}

	function writeStoredWidths(key: string, widths: number[]) {
		try {
			localStorage.setItem(STORAGE_PREFIX + key, JSON.stringify(widths));
		} catch {
			// ignore quota / private mode
		}
	}

	/** Locks measured widths when `widths` is omitted; applies provided widths when present. */
	function applyColumnWidths(
		table: HTMLTableElement,
		cells: HTMLTableCellElement[],
		widths?: number[]
	) {
		let total = 0;
		cells.forEach((th, i) => {
			const width = Math.max(
				MIN_COL_WIDTH,
				widths ? widths[i]! : th.getBoundingClientRect().width
			);
			th.style.width = `${width}px`;
			th.style.minWidth = `${MIN_COL_WIDTH}px`;
			total += width;
		});
		table.style.width = `${total}px`;
	}

	function resizableColumns(persistKey: string | undefined): Attachment<HTMLTableElement> {
		return table => {
			table.style.tableLayout = 'fixed';

			const cells = contentHeaderCells(table);
			if (cells.length === 0) return;

			const stored = persistKey ? readStoredWidths(persistKey) : null;
			let widthsLocked = false;

			if (stored && stored.length === cells.length) {
				applyColumnWidths(table, cells, stored);
				widthsLocked = true;
			}

			const cleanups: (() => void)[] = [];

			for (const th of cells) {
				const handle = document.createElement('span');
				handle.className = 'table-col-resize-handle';
				handle.setAttribute('aria-hidden', 'true');
				handle.tabIndex = -1;

				const onPointerDown = (event: PointerEvent) => {
					if (event.button !== 0) return;
					event.preventDefault();
					event.stopPropagation();

					if (!widthsLocked) {
						applyColumnWidths(table, cells);
						widthsLocked = true;
					}

					const startX = event.clientX;
					const startWidth = th.getBoundingClientRect().width;
					const startTableWidth = table.getBoundingClientRect().width;

					handle.dataset.active = '';
					handle.setPointerCapture(event.pointerId);
					document.body.style.cursor = 'col-resize';
					document.body.style.userSelect = 'none';

					const onPointerMove = (moveEvent: PointerEvent) => {
						const nextWidth = Math.max(MIN_COL_WIDTH, startWidth + (moveEvent.clientX - startX));
						const delta = nextWidth - startWidth;
						th.style.width = `${nextWidth}px`;
						table.style.width = `${startTableWidth + delta}px`;
					};

					const onPointerUp = (upEvent: PointerEvent) => {
						handle.releasePointerCapture(upEvent.pointerId);
						delete handle.dataset.active;
						document.body.style.cursor = '';
						document.body.style.userSelect = '';
						handle.removeEventListener('pointermove', onPointerMove);
						handle.removeEventListener('pointerup', onPointerUp);
						handle.removeEventListener('pointercancel', onPointerUp);

						if (persistKey) {
							writeStoredWidths(
								persistKey,
								cells.map(cell => Math.max(MIN_COL_WIDTH, cell.getBoundingClientRect().width))
							);
						}
					};

					handle.addEventListener('pointermove', onPointerMove);
					handle.addEventListener('pointerup', onPointerUp);
					handle.addEventListener('pointercancel', onPointerUp);
				};

				handle.addEventListener('pointerdown', onPointerDown);
				th.appendChild(handle);
				cleanups.push(() => {
					handle.removeEventListener('pointerdown', onPointerDown);
					handle.remove();
				});
			}

			return () => {
				for (const cleanup of cleanups) cleanup();
				for (const th of cells) {
					th.style.width = '';
					th.style.minWidth = '';
				}
				table.style.width = '';
				table.style.tableLayout = '';
			};
		};
	}
</script>

<section {...rest} class={['card', className]}>
	<div class="card-header flex items-center justify-between gap-3">
		<h2 class="text-lg font-medium">{title}</h2>
		{#if actions}
			<div class="shrink-0">{@render actions()}</div>
		{/if}
	</div>

	{#if items.length === 0}
		<p class="p-5 text-hint">{empty}</p>
	{:else}
		<div class="overflow-x-auto">
			<table
				class="table-resizable min-w-full text-sm"
				{@attach resizableColumns(storageKey ?? title)}
			>
				<thead class="border-b border-border text-left text-label">
					<tr>
						{@render header()}
						{#if getHref}
							<th class="w-0 p-0" aria-hidden="true"></th>
						{/if}
					</tr>
				</thead>
				<tbody class="divide-y divide-border">
					{#each items as item (getKey(item))}
						{@const href = getHref?.(item)}
						<tr
							class={
								href
									? 'relative cursor-pointer transition-colors hover:bg-surface-hover focus-within:bg-surface-hover'
									: undefined
							}
						>
							{@render row(item)}
							{#if href}
								<td class="w-0 p-0">
									<a
										href={href}
										class="absolute inset-0 focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-primary"
										aria-label={String(getKey(item))}
									></a>
								</td>
							{/if}
						</tr>
					{/each}
				</tbody>
			</table>
		</div>
	{/if}

	{#if overlay}
		{@render overlay()}
	{/if}
</section>

<style>
	.table-resizable :global(th:not([aria-hidden='true'])) {
		position: relative;
	}

	.table-resizable :global(.table-col-resize-handle) {
		position: absolute;
		top: 0;
		right: 0;
		z-index: 1;
		width: 5px;
		height: 100%;
		cursor: col-resize;
		touch-action: none;
		user-select: none;
		background: transparent;
	}

	.table-resizable :global(.table-col-resize-handle:hover),
	.table-resizable :global(.table-col-resize-handle[data-active]) {
		background: color-mix(in srgb, var(--color-primary) 28%, transparent);
		box-shadow: inset -1px 0 0 var(--color-border);
	}
</style>
