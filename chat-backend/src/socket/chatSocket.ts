import { Server } from "socket.io";
import type { Server as IOServer } from "socket.io";
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
  const match = cookieHeader
    .split(";")
    .map((c) => c.trim())
    .find((c) => c.startsWith(`${name}=`));
  return match?.split("=").slice(1).join("=");
}

export function createSocketServer(
  server: unknown,
): IOServer {
  const CORS_ORIGIN =
    process.env.CORS_ORIGIN || "http://localhost:5173";

  io = new Server(server as never, {
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
    socket.on(
      "send_message",
      async (data: {
        chatId: number;
        content: string;
      }) => {
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
      },
    );

    // Typing indicator
    socket.on(
      "typing",
      async (data: { chatId: number }) => {
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
      },
    );

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
