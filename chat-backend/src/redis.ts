import { createClient } from "redis";

const REDIS_URL = process.env.REDIS_URL || "redis://localhost:6379";

const redis = createClient({ url: REDIS_URL });

redis.on("error", (err) => {
	console.error("Redis connection error:", err.message);
});

await redis.connect();
console.log("Connected to Redis");

export default redis;
