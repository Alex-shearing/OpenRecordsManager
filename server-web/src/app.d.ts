// See https://svelte.dev/docs/kit/types#app.d.ts
// for information about these interfaces
declare global {
	namespace App {
		// interface Error {}
		// interface Locals {}
		// interface PageData {}
		// interface PageState {}
		// interface Platform {}
	}

	interface Window {
		__ORM_UI__?: {
			/** Empty string = same origin (relative API calls). */
			apiBaseUrl?: string;
		};
	}
}

export {};
