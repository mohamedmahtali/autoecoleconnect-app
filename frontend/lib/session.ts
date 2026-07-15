// Lecture de la session côté serveur (Server Components, route handlers).
import { cookies } from "next/headers";

import { decoderJwt, SESSION_COOKIE, sessionValide, type SessionPayload } from "@/lib/auth";

export async function getSession(): Promise<SessionPayload | null> {
  const token = (await cookies()).get(SESSION_COOKIE)?.value;
  if (!token) {
    return null;
  }
  const session = decoderJwt(token);
  return sessionValide(session) ? session : null;
}

export async function getToken(): Promise<string | null> {
  return (await cookies()).get(SESSION_COOKIE)?.value ?? null;
}
