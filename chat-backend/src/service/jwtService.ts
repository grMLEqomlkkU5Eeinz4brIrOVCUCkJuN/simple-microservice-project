import * as jose from "jose";

const JWT_SECRET = process.env.JWT_SECRET || "change-me-in-production";

export interface AuthUser {
  id: number;
  email: string;
}

export async function validateToken(
  token: string,
): Promise<AuthUser | null> {
  try {
    const secret = new TextEncoder().encode(JWT_SECRET);
    const { payload } = await jose.jwtVerify(token, secret, {
      issuer: "todo-api",
      algorithms: ["HS256"],
    });

    const id = Number(payload.sub);
    const email = payload.email as string;

    if (!id || !email) return null;
    return { id, email };
  } catch {
    return null;
  }
}
