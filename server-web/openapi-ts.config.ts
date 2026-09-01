import { defineConfig } from '@hey-api/openapi-ts';

export default defineConfig({
	input: 'http://localhost:8080/v3/api-docs',
	output: 'src/lib/api',
	plugins: [
		'@hey-api/typescript',
		{
			name: '@hey-api/sdk',
			operations: {
				strategy: 'byTags',
			},
		},
		{
			name: '@hey-api/client-fetch',
			runtimeConfigPath: '../api-client.config.ts',
		},
	],
});
