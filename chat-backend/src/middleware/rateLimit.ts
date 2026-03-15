import { Elysia } from "elysia";

const requestCounts = new Map<string, { count: number; resetAt: number }>();

// Clean up expired entries every 60 seconds
setInterval(() => {
	const now = Date.now();
	for (const [key, value] of requestCounts) {
		if (value.resetAt <= now) {
			requestCounts.delete(key);
		}
	}
}, 60_000);

export function rateLimit(maxRequests: number, windowMs: number) {
	return new Elysia({ name: "rate-limit" }).derive(
		{ as: "scoped" },
		({ request, set }) => {
			const ip =
				request.headers.get("x-forwarded-for")?.split(",")[0]?.trim() ??
				"unknown";
			const key = `${ip}:${new URL(request.url).pathname}`;
			const now = Date.now();

			const entry = requestCounts.get(key);
			if (entry && entry.resetAt > now) {
				if (entry.count >= maxRequests) {
					set.status = 429;
					throw new Error("Too many requests");
				}
				entry.count++;
			} else {
				requestCounts.set(key, {
					count: 1,
					resetAt: now + windowMs,
				});
			}

			return {};
		},
	);
}
