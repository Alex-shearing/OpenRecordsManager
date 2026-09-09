import tailwindcss from '@tailwindcss/vite';
import adapter from '@sveltejs/adapter-static';
import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';

export default defineConfig({
	plugins: [
		tailwindcss(),
		sveltekit({
			compilerOptions: {
				// Force runes mode for the project, except for libraries. Can be removed in svelte 6.
				runes: ({ filename }) => (filename.split(/[/\\]/).includes('node_modules') ? undefined : true),
			},
			adapter: adapter({
				fallback: 'index.html',
			}),
		}),
	],
	// Used during development to proxy API requests to the Spring Boot backend.
	// Keep changeOrigin false so Host stays the Vite origin (e.g. localhost:5173).
	// Spring treats Origin≠Host as CORS; with an empty allow-list that becomes 403
	// "Invalid CORS request" on login and other browser POSTs.
	server: {
		proxy: {
			'/api': {
				target: 'http://localhost:8080',
				changeOrigin: false,
				secure: false,
			},
		},
	},
});
