import { Elysia } from "elysia";
import { swagger } from "@elysiajs/swagger";
import { cors } from "@elysiajs/cors";
import { routes } from "./routes";
import { createSocketServer } from "./socket/chatSocket";
import prisma from "./db";
import redis from "./redis";
import type { Server as HttpServer } from "node:http";

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
      if (error.message === "Too many requests") {
        set.status = 429;
        return {
          error: "RATE_LIMITED",
          message: "Too many requests. Please try again later.",
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

const httpServer = app.server as unknown as HttpServer;
const io = createSocketServer(httpServer);

console.log(
  `Chat backend started at http://localhost:${PORT}`,
);
console.log("Socket.io server attached");

// Graceful shutdown
async function shutdown() {
  console.log("Shutting down gracefully...");
  io.close();
  await prisma.$disconnect();
  await redis.quit();
  process.exit(0);
}

process.on("SIGTERM", shutdown);
process.on("SIGINT", shutdown);

export { app, io };
