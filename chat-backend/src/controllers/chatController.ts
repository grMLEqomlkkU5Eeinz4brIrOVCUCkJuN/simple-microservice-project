import type { AuthUser } from "../service/jwtService";
import { userExists } from "../service/userService";
import prisma from "../db";
import type { Chat, Message } from "../generated/prisma/client";

interface MessagesResult {
  messages: Message[];
  nextCursor: number | null;
}
interface SuccessResult<T> {
  data: T;
  status: number;
}

interface ErrorResult {
  error: string;
  status: number;
}

type Result<T> = SuccessResult<T> | ErrorResult;

export async function createOrGetChat(
  user: AuthUser,
  participantId: number,
): Promise<Result<Chat>> {
  if (participantId === user.id) {
    return {
      error: "Cannot create a chat with yourself",
      status: 400,
    };
  }

  const targetExists = await userExists(participantId);
  if (!targetExists) {
    return { error: "Target user not found", status: 404 };
  }

  // Check if a DM already exists between these two users
  const existingChat = await prisma.chat.findFirst({
    where: {
      AND: [
        {
          participants: { some: { userId: user.id } },
        },
        {
          participants: {
            some: { userId: participantId },
          },
        },
      ],
    },
    include: { participants: true },
  });

  if (existingChat) {
    return { data: existingChat, status: 200 };
  }

  const chat = await prisma.chat.create({
    data: {
      participants: {
        create: [
          { userId: user.id },
          { userId: participantId },
        ],
      },
    },
    include: { participants: true },
  });

  return { data: chat, status: 201 };
}

export async function listChats(
  user: AuthUser,
): Promise<Result<Chat[]>> {
  const chats = await prisma.chat.findMany({
    where: {
      participants: { some: { userId: user.id } },
    },
    include: {
      participants: true,
      messages: {
        orderBy: { createdAt: "desc" },
        take: 1,
      },
    },
    orderBy: { updatedAt: "desc" },
  });

  return { data: chats, status: 200 };
}

export async function getMessages(
  user: AuthUser,
  chatId: number,
  cursor?: number,
  limit: number = 50,
): Promise<Result<MessagesResult>> {
  const participant =
    await prisma.chatParticipant.findUnique({
      where: {
        userId_chatId: { userId: user.id, chatId },
      },
    });

  if (!participant) {
    return { error: "Access denied", status: 403 };
  }

  const messages = await prisma.message.findMany({
    where: { chatId },
    orderBy: { createdAt: "desc" },
    take: limit,
    ...(cursor
      ? { cursor: { id: cursor }, skip: 1 }
      : {}),
  });

  return {
    data: {
      messages,
      nextCursor:
        messages.length === limit
          ? messages[messages.length - 1]?.id ?? null
          : null,
    },
    status: 200,
  };
}

export async function sendMessage(
  user: AuthUser,
  chatId: number,
  content: string,
): Promise<Result<Message>> {
  if (!content || content.trim().length === 0) {
    return {
      error: "Message content is required",
      status: 400,
    };
  }

  const participant =
    await prisma.chatParticipant.findUnique({
      where: {
        userId_chatId: { userId: user.id, chatId },
      },
    });

  if (!participant) {
    return { error: "Access denied", status: 403 };
  }

  const message = await prisma.message.create({
    data: {
      fromUserId: user.id,
      chatId,
      chatContent: content.trim(),
    },
  });

  await prisma.chat.update({
    where: { id: chatId },
    data: { updatedAt: new Date() },
  });

  return { data: message, status: 201 };
}
