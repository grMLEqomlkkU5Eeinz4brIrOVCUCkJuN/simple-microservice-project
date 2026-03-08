import redis from "../redis";

const USER_SERVICE_URL =
	process.env.USER_SERVICE_URL || "http://localhost:8080";

const CACHE_TTL_SECONDS = 5 * 60; // 5 minutes

export interface UserInfo {
	id: number;
	email: string;
	name: string;
}

async function fetchAndCacheUser(
	userId: number,
): Promise<UserInfo | null> {
	const cacheKey = `user:${userId}`;
	const cached = await redis.get(cacheKey);
	if (cached !== null) {
		if (cached === "miss") return null;
		return JSON.parse(cached) as UserInfo;
	}

	try {
		const response = await fetch(
			`${USER_SERVICE_URL}/api/user/${userId}`,
		);
		if (response.status !== 200) {
			await redis.setEx(cacheKey, CACHE_TTL_SECONDS, "miss");
			return null;
		}
		const data = (await response.json()) as UserInfo;
		const user: UserInfo = {
			id: data.id,
			email: data.email,
			name: data.name,
		};
		await redis.setEx(
			cacheKey,
			CACHE_TTL_SECONDS,
			JSON.stringify(user),
		);
		return user;
	} catch {
		return null;
	}
}

export async function userExists(userId: number): Promise<boolean> {
	const user = await fetchAndCacheUser(userId);
	return user !== null;
}

export async function getUserInfo(
	userId: number,
): Promise<UserInfo | null> {
	return fetchAndCacheUser(userId);
}