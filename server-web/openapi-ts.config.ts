import { defineConfig } from '@hey-api/openapi-ts';

export default defineConfig({
	input: 'http://localhost:8080/v3/api-docs',
	output: 'src/lib/api',
	plugins: [
		{
			name: '@hey-api/typescript',
			enums: 'javascript',
		},
		{
			name: '@hey-api/sdk',
			client: false,
			operations: {
				strategy: 'byTags',
			},
		},
		'@hey-api/client-fetch',
	],
});
