// place files you want to import through the `$lib` alias in this folder.
import createClient from "openapi-fetch";
import type { paths } from "./types/schema";

export const client = createClient<paths>({ baseUrl: "/api" });