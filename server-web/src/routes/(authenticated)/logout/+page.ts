import { goto } from "$app/navigation";
import { AuthController } from "$lib";

await AuthController.logout();
await goto('/login')