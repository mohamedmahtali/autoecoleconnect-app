import { NextResponse } from "next/server";

import { BACKEND_URL } from "@/lib/api";
import { SESSION_COOKIE } from "@/lib/auth";
import type { LoginBackendResponse } from "@/types/api";

// BFF : le JWT émis par le backend est stocké dans un cookie httpOnly —
// jamais exposé au JavaScript du navigateur (protection XSS).
export async function POST(request: Request) {
  const identifiants = await request.json();
  const res = await fetch(`${BACKEND_URL}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(identifiants),
    cache: "no-store",
  });
  const corps = await res.json();
  if (!res.ok) {
    return NextResponse.json(corps, { status: res.status });
  }

  const login = corps as LoginBackendResponse;
  const reponse = NextResponse.json({
    role: login.role,
    nomComplet: login.nomComplet,
  });
  reponse.cookies.set(SESSION_COOKIE, login.token, {
    httpOnly: true,
    sameSite: "lax",
    path: "/",
    expires: new Date(login.expireLe),
    // secure activé quand le TLS terminera devant le pod (cert-manager, Phase 1)
    secure: process.env.COOKIE_SECURE === "true",
  });
  return reponse;
}
