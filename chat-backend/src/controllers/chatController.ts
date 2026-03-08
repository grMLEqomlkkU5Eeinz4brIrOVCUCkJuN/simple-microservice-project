import type { AuthUser } from "../service/jwtService";
import {
  userExists,
  getUserInfo,
  type UserInfo,
} from "../service/userService";
import prisma from "../db";
import type {
  Chat,
  Message,
} from "../generated/prisma/client";

interface MessagesResult {
  messages: Message[];
  nextCursor: number | null;
}

interface ParticipantInfo {
  userId: number;
  name: string | null;
  email: string | null;
}

interface ChatWithDetails extends Chat {
  participants: ParticipantInfo[];
  messages?: Message[];
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

async function enrichParticipants(
  participants: { userId: number }[],
): Promise<ParticipantInfo[]> {
  return Promise.all(
    participants.map(async (p) => {
      const info = await getUserInfo(p.userId);
      return {
        userId: p.userId,
        name: info?.name ?? null,
        email: info?.email ?? null,
      };
    }),
  );
}

export async function createOrGetChat(
  user: AuthUser,
  participantId: number,
): Promise<Result<ChatWithDetails>> {
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
    const participants = await enrichParticipants(
      existingChat.participants,
    );
    return {
      data: { ...existingChat, participants },
      status: 200,
    };
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

  const participants = await enrichParticipants(
    chat.participants,
  );
  return {
    data: { ...chat, participants },
    status: 201,
  };
}

export async function getChat(
  user: AuthUser,
  chatId: number,
): Promise<Result<ChatWithDetails>> {
  const chat = await prisma.chat.findUnique({
    where: { id: chatId },
    include: {
      participants: true,
      messages: {
        orderBy: { createdAt: "desc" },
        take: 1,
      },
    },
  });

  if (!chat) {
    return { error: "Chat not found", status: 404 };
  }

  const isParticipant = chat.participants.some(
    (p) => p.userId === user.id,
  );
  if (!isParticipant) {
    return { error: "Access denied", status: 403 };
  }

  const participants = await enrichParticipants(
    chat.participants,
  );
  return {
    data: { ...chat, participants },
    status: 200,
  };
}

export async function listChats(
  user: AuthUser,
): Promise<Result<ChatWithDetails[]>> {
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

  const enriched = await Promise.all(
    chats.map(async (chat) => {
      const participants = await enrichParticipants(
        chat.participants,
      );
      return { ...chat, participants };
    }),
  );

  return { data: enriched, status: 200 };
}

export async function deleteChat(
  user: AuthUser,
  chatId: number,
): Promise<Result<{ message: string }>> {
  const participant =
    await prisma.chatParticipant.findUnique({
      where: {
        userId_chatId: { userId: user.id, chatId },
      },
    });

  if (!participant) {
    return { error: "Access denied", status: 403 };
  }

  // Remove user from chat
  await prisma.chatParticipant.delete({
    where: {
      userId_chatId: { userId: user.id, chatId },
    },
  });

  // If no participants remain, delete the chat entirely
  const remaining =
    await prisma.chatParticipant.count({
      where: { chatId },
    });

  if (remaining === 0) {
    await prisma.chat.delete({
      where: { id: chatId },
    });
  }

  return {
    data: { message: "Left chat successfully" },
    status: 200,
  };
}

export async function getParticipants(
  user: AuthUser,
  chatId: number,
): Promise<Result<ParticipantInfo[]>> {
  const participant =
    await prisma.chatParticipant.findUnique({
      where: {
        userId_chatId: { userId: user.id, chatId },
      },
    });

  if (!participant) {
    return { error: "Access denied", status: 403 };
  }

  const participants =
    await prisma.chatParticipant.findMany({
      where: { chatId },
    });

  const enriched =
    await enrichParticipants(participants);
  return { data: enriched, status: 200 };
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

  if (content.length > 5000) {
    return {
      error: "Message content exceeds maximum length of 5000 characters",
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
