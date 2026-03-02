import { PrismaClient } from "./src/generated/prisma/client";
import { PrismaMariaDb } from "@prisma/adapter-mariadb";

const adapter = new PrismaMariaDb({
  host: process.env.DB_HOST,
  port: parseInt(process.env.DB_PORT as string),
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME,
  connectionLimit: parseInt(
    process.env.DB_CONNECTION_LIMIT as string,
  ),
});
const prisma = new PrismaClient({ adapter });

const main = async (): Promise<void> => {
  // Create a test chat between user 1 and user 2
  const chat = await prisma.chat.create({
    data: {
      participants: {
        create: [{ userId: 1 }, { userId: 2 }],
      },
    },
  });

  // Add some test messages
  await prisma.message.createMany({
    data: [
      {
        fromUserId: 1,
        chatId: chat.id,
        chatContent: "Hello! This is a test message.",
      },
      {
        fromUserId: 2,
        chatId: chat.id,
        chatContent: "Hi! Got your message.",
      },
    ],
  });

  console.log("Seed data created successfully");
};

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
