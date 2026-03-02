const USER_SERVICE_URL =
  process.env.USER_SERVICE_URL || "http://localhost:8080";

const cache = new Map<
  number,
  { exists: boolean; timestamp: number }
>();
const CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

export async function userExists(
  userId: number,
): Promise<boolean> {
  const now = Date.now();
  const cached = cache.get(userId);
  if (cached && now - cached.timestamp < CACHE_TTL_MS) {
    return cached.exists;
  }

  try {
    const response = await fetch(
      `${USER_SERVICE_URL}/api/user/${userId}`,
    );
    const exists = response.status === 200;
    cache.set(userId, { exists, timestamp: now });
    return exists;
  } catch {
    return false;
  }
}
