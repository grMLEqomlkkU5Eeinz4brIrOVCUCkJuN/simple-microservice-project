import { Elysia } from "elysia";
import { chatRoutes } from "./chatRoutes";

export const routes = new Elysia().use(chatRoutes);
