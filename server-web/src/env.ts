import { defineEnvVars } from '@sveltejs/kit/env';

export const variables = defineEnvVars({
	PUBLIC_API_URL: {
		public: true,
		static: false,
		description: 'The API backend endpoint',
		schema: (value: string | undefined) => {
			return value ?? '';
		},
	},
});
