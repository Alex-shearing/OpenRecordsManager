import { UserController } from "$lib";

export async function load() {
    const { data } = await UserController.me();
    const me = data?.success ? data.data : ({
        username: 'Me'
    });

    return {
        me
    };
}