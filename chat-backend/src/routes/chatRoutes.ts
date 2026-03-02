import { Elysia, t } from "elysia";
import { authMiddleware } from "../middleware/auth";
import {
  createOrGetChat,
  listChats,
  getMessages,
  sendMessage,
} from "../controllers/chatController";
import { getIO } from "../socket/chatSocket";
import prisma from "../db";

export const chatRoutes = new Elysia({
  prefix: "/api/chats",
})
  .use(authMiddleware)
  .post(
    "/",
    async ({ user, body, set }) => {
      const result = await createOrGetChat(
        user,
        body.participantId,
      );
      set.status = result.status;
      if ("error" in result) return { error: result.error };
      return result.data;
    },
    {
      body: t.Object({
        participantId: t.Number(),
      }),
    },
  )
  .get("/", async ({ user, set }) => {
    const result = await listChats(user);
    set.status = result.status;
    if ("error" in result) return { error: result.error };
    return result.data;
  })
  .get(
    "/:chatId/messages",
    async ({ user, params, query, set }) => {
      const chatId = Number(params.chatId);
      const cursor = query.cursor
        ? Number(query.cursor)
        : undefined;
      const limit = query.limit
        ? Number(query.limit)
        : 50;

      const result = await getMessages(
        user,
        chatId,
        cursor,
        limit,
      );
      set.status = result.status;
      if ("error" in result) return { error: result.error };
      return result.data;
    },
    {
      query: t.Object({
        cursor: t.Optional(t.String()),
        limit: t.Optional(t.String()),
      }),
    },
  )
  .post(
    "/:chatId/messages",
    async ({ user, params, body, set }) => {
      const chatId = Number(params.chatId);
      const result = await sendMessage(
        user,
        chatId,
        body.content,
      );
      set.status = result.status;
      if ("error" in result) return { error: result.error };

      // Broadcast via Socket.io
      const io = getIO();
      if (io) {
        const participants =
          await prisma.chatParticipant.findMany({
            where: { chatId },
          });
        for (const p of participants) {
          if (p.userId !== user.id) {
            io.to(`user:${p.userId}`).emit(
              "message_received",
              result.data,
            );
          }
        }
      }

      return result.data;
    },
    {
      body: t.Object({
        content: t.String(),
      }),
    },
  );
