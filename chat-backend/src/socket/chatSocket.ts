import { Server } from "socket.io";
import type { Server as IOServer } from "socket.io";
import type { Server as HttpServer } from "node:http";
import {
  validateToken,
  type AuthUser,
} from "../service/jwtService";
import { userExists } from "../service/userService";
import { sendMessage } from "../controllers/chatController";
import prisma from "../db";

let io: IOServer | null = null;

const userSockets = new Map<number, Set<string>>();

export function getIO(): IOServer | null {
  return io;
}

function parseCookie(
  cookieHeader: string,
  name: string,
): string | undefined {
  const cookies = cookieHeader.split(";");
  for (const cookie of cookies) {
    const [key, ...valueParts] = cookie.trim().split("=");
    if (key === name) {
      const value = valueParts.join("=");
      try {
        return decodeURIComponent(value);
      } catch {
        return value;
      }
    }
  }
  return undefined;
}

function isValidSendMessage(
  data: unknown,
): data is { chatId: number; content: string } {
  if (typeof data !== "object" || data === null)
    return false;
  const d = data as Record<string, unknown>;
  return (
    typeof d.chatId === "number" &&
    Number.isInteger(d.chatId) &&
    d.chatId > 0 &&
    typeof d.content === "string" &&
    d.content.trim().length > 0 &&
    d.content.length <= 5000
  );
}

function isValidTyping(
  data: unknown,
): data is { chatId: number } {
  if (typeof data !== "object" || data === null)
    return false;
  const d = data as Record<string, unknown>;
  return (
    typeof d.chatId === "number" &&
    Number.isInteger(d.chatId) &&
    d.chatId > 0
  );
}

// Simple per-user WebSocket rate limiter
const messageRateLimits = new Map<number, { count: number; resetAt: number }>();
const MAX_MESSAGES_PER_WINDOW = 30;
const RATE_WINDOW_MS = 10_000;

function isRateLimited(userId: number): boolean {
  const now = Date.now();
  const entry = messageRateLimits.get(userId);
  if (entry && entry.resetAt > now) {
    if (entry.count >= MAX_MESSAGES_PER_WINDOW) return true;
    entry.count++;
    return false;
  }
  messageRateLimits.set(userId, { count: 1, resetAt: now + RATE_WINDOW_MS });
  return false;
}

export function createSocketServer(
  server: HttpServer,
): IOServer {
  const CORS_ORIGIN =
    process.env.CORS_ORIGIN || "http://localhost:5173";

  io = new Server(server, {
    cors: {
      origin: CORS_ORIGIN,
      credentials: true,
    },
  });

  // Authentication middleware
  io.use(async (socket, next) => {
    try {
      const cookieHeader =
        socket.handshake.headers.cookie || "";
      const authToken = parseCookie(
        cookieHeader,
        "auth_token",
      );

      if (!authToken) {
        return next(new Error("Authentication required"));
      }

      const user = await validateToken(authToken);
      if (!user) {
        return next(new Error("Invalid token"));
      }

      const exists = await userExists(user.id);
      if (!exists) {
        return next(new Error("User not found"));
      }

      socket.data = { user };
      next();
    } catch {
      next(new Error("Authentication failed"));
    }
  });

  io.on("connection", (socket) => {
    const user = socket.data.user as AuthUser;

    // Track socket for this user
    if (!userSockets.has(user.id)) {
      userSockets.set(user.id, new Set());
    }
    userSockets.get(user.id)!.add(socket.id);

    socket.join(`user:${user.id}`);

    console.log(
      `User ${user.id} connected (socket: ${socket.id})`,
    );

    // Handle sending messages
    socket.on("send_message", async (data: unknown) => {
      if (isRateLimited(user.id)) {
        socket.emit("error", {
          message: "Rate limited. Please slow down.",
        });
        return;
      }

      if (!isValidSendMessage(data)) {
        socket.emit("error", {
          message:
            "Invalid message data: chatId (positive integer) and content (1-5000 chars) required",
        });
        return;
      }

      const { chatId, content } = data;

      const result = await sendMessage(
        user,
        chatId,
        content,
      );

      if ("error" in result) {
        socket.emit("error", {
          message: result.error,
        });
        return;
      }

      // Find other participants and emit
      const participants =
        await prisma.chatParticipant.findMany({
          where: { chatId },
        });

      const otherParticipantIds = participants
        .map((p) => p.userId)
        .filter((id) => id !== user.id);

      const messagePayload = {
        ...result.data,
        chatId,
      };

      for (const participantId of otherParticipantIds) {
        io!
          .to(`user:${participantId}`)
          .emit("message_received", messagePayload);
      }

      socket.emit("message_sent", messagePayload);
    });

    // Typing indicator
    socket.on("typing", async (data: unknown) => {
      if (!isValidTyping(data)) return;

      const { chatId } = data;

      const participant =
        await prisma.chatParticipant.findUnique({
          where: {
            userId_chatId: {
              userId: user.id,
              chatId,
            },
          },
        });

      if (!participant) return;

      const participants =
        await prisma.chatParticipant.findMany({
          where: {
            chatId,
            userId: { not: user.id },
          },
        });

      for (const p of participants) {
        io!.to(`user:${p.userId}`).emit("user_typing", {
          chatId,
          userId: user.id,
        });
      }
    });

    // Stop typing indicator
    socket.on("stop_typing", async (data: unknown) => {
      if (!isValidTyping(data)) return;

      const { chatId } = data;

      const participant =
        await prisma.chatParticipant.findUnique({
          where: {
            userId_chatId: {
              userId: user.id,
              chatId,
            },
          },
        });

      if (!participant) return;

      const participants =
        await prisma.chatParticipant.findMany({
          where: {
            chatId,
            userId: { not: user.id },
          },
        });

      for (const p of participants) {
        io!
          .to(`user:${p.userId}`)
          .emit("user_stop_typing", {
            chatId,
            userId: user.id,
          });
      }
    });

    socket.on("disconnect", () => {
      const sockets = userSockets.get(user.id);
      if (sockets) {
        sockets.delete(socket.id);
        if (sockets.size === 0) {
          userSockets.delete(user.id);
        }
      }
      console.log(
        `User ${user.id} disconnected (socket: ${socket.id})`,
      );
    });
  });

  return io;
}
