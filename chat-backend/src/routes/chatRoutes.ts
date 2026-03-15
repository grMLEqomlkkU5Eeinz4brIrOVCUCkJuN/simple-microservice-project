import { Elysia, t } from "elysia";
import { authMiddleware } from "../middleware/auth";
import { rateLimit } from "../middleware/rateLimit";
import {
  createOrGetChat,
  getChat,
  listChats,
  deleteChat,
  getParticipants,
  getMessages,
  sendMessage,
} from "../controllers/chatController";
import { getIO } from "../socket/chatSocket";
import prisma from "../db";

export const chatRoutes = new Elysia({
  prefix: "/api/chats",
})
  .use(authMiddleware)
  .use(rateLimit(60, 60_000))
  .post(
    "/",
    async ({ user, body, set }) => {
      const result = await createOrGetChat(
        user,
        body.participantId,
      );
      set.status = result.status;
      if ("error" in result)
        return { error: result.error };
      return result.data;
    },
    {
      body: t.Object({
        participantId: t.Number({ minimum: 1 }),
      }),
    },
  )
  .get("/", async ({ user, set }) => {
    const result = await listChats(user);
    set.status = result.status;
    if ("error" in result)
      return { error: result.error };
    return result.data;
  })
  .get(
    "/:chatId",
    async ({ user, params, set }) => {
      const chatId = Number(params.chatId);
      if (!Number.isInteger(chatId) || chatId < 1) {
        set.status = 400;
        return { error: "Invalid chat ID" };
      }

      const result = await getChat(user, chatId);
      set.status = result.status;
      if ("error" in result)
        return { error: result.error };
      return result.data;
    },
  )
  .delete(
    "/:chatId",
    async ({ user, params, set }) => {
      const chatId = Number(params.chatId);
      if (!Number.isInteger(chatId) || chatId < 1) {
        set.status = 400;
        return { error: "Invalid chat ID" };
      }

      const result = await deleteChat(user, chatId);
      set.status = result.status;
      if ("error" in result)
        return { error: result.error };
      return result.data;
    },
  )
  .get(
    "/:chatId/participants",
    async ({ user, params, set }) => {
      const chatId = Number(params.chatId);
      if (!Number.isInteger(chatId) || chatId < 1) {
        set.status = 400;
        return { error: "Invalid chat ID" };
      }

      const result = await getParticipants(
        user,
        chatId,
      );
      set.status = result.status;
      if ("error" in result)
        return { error: result.error };
      return result.data;
    },
  )
  .get(
    "/:chatId/messages",
    async ({ user, params, query, set }) => {
      const chatId = Number(params.chatId);
      if (!Number.isInteger(chatId) || chatId < 1) {
        set.status = 400;
        return { error: "Invalid chat ID" };
      }

      const cursor = query.cursor
        ? Number(query.cursor)
        : undefined;
      if (
        cursor !== undefined &&
        (!Number.isInteger(cursor) || cursor < 1)
      ) {
        set.status = 400;
        return { error: "Invalid cursor" };
      }

      let limit = query.limit
        ? Number(query.limit)
        : 50;
      if (
        !Number.isInteger(limit) ||
        limit < 1 ||
        limit > 100
      ) {
        limit = 50;
      }

      const result = await getMessages(
        user,
        chatId,
        cursor,
        limit,
      );
      set.status = result.status;
      if ("error" in result)
        return { error: result.error };
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
      if (!Number.isInteger(chatId) || chatId < 1) {
        set.status = 400;
        return { error: "Invalid chat ID" };
      }

      const result = await sendMessage(
        user,
        chatId,
        body.content,
      );
      set.status = result.status;
      if ("error" in result)
        return { error: result.error };

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
        content: t.String({
          minLength: 1,
          maxLength: 5000,
        }),
      }),
    },
  );
