import { Elysia } from "elysia";
import {
  validateToken,
  type AuthUser,
} from "../service/jwtService";
import { userExists } from "../service/userService";

export const authMiddleware = new Elysia({
  name: "auth",
}).derive(
  { as: "scoped" },
  async ({
    cookie,
    set,
  }): Promise<{ user: AuthUser }> => {
    const token = cookie.auth_token?.value;

    if (!token) {
      set.status = 401;
      throw new Error("Missing authentication token");
    }

    const user = await validateToken(token as string);
    if (!user) {
      set.status = 401;
      throw new Error("Invalid authentication token");
    }

    const exists = await userExists(user.id);
    if (!exists) {
      set.status = 401;
      throw new Error("User not found");
    }

    return { user };
  },
);
