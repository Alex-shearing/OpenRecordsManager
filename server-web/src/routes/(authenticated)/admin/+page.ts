import { goto } from '$app/navigation';

export async function load({}) {
	throw goto('/admin/config');
}
