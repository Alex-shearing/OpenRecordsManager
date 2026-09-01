export async function load({ url }) {
	return {
		type: url.searchParams.get('type'),
		q: url.searchParams.get('q'),
	};
}
