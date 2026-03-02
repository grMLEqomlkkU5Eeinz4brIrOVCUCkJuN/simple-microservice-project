import { Elysia } from "elysia";
import { swagger } from "@elysiajs/swagger";
import { cors } from "@elysiajs/cors";
import { routes } from "./routes";
import { createSocketServer } from "./socket/chatSocket";

const PORT = parseInt(process.env.PORT as string) || 3001;
const CORS_ORIGIN =
  process.env.CORS_ORIGIN || "http://localhost:5173";

const app = new Elysia()
  .onError(({ code, error, set }) => {
    if (code === "UNKNOWN") {
      if (
        error.message === "Missing authentication token" ||
        error.message === "Invalid authentication token" ||
        error.message === "User not found"
      ) {
        set.status = 401;
        return {
          error: "UNAUTHORIZED",
          message: error.message,
        };
      }
    }
  })
  .use(
    cors({
      origin: CORS_ORIGIN,
      credentials: true,
      allowedHeaders: ["Content-Type"],
      methods: [
        "GET",
        "POST",
        "PUT",
        "DELETE",
        "OPTIONS",
      ],
    }),
  )
  .use(swagger())
  .use(routes)
  .listen(PORT);

const io = createSocketServer(app.server);

console.log(
  `Chat backend started at http://localhost:${PORT}`,
);
console.log("Socket.io server attached");

export { app, io };
