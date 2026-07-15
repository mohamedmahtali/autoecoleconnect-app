import { NextResponse, type NextRequest } from "next/server";

import { decoderJwt, SESSION_COOKIE, sessionValide } from "@/lib/auth";

// Garde d'accès des espaces authentifiés. La vérification cryptographique du
// token reste du ressort du backend ; ici on aiguille (session présente,
// non expirée, bon rôle) avant même de rendre la page.
export function middleware(request: NextRequest) {
  const token = request.cookies.get(SESSION_COOKIE)?.value;
  const session = token ? decoderJwt(token) : null;

  if (!sessionValide(session)) {
    const login = new URL("/login", request.url);
    login.searchParams.set("redirige", request.nextUrl.pathname);
    return NextResponse.redirect(login);
  }
  if (request.nextUrl.pathname.startsWith("/admin") && session.role !== "DIRECTEUR") {
    return NextResponse.redirect(new URL("/login", request.url));
  }
  return NextResponse.next();
}

export const config = {
  matcher: ["/admin/:path*"],
};
