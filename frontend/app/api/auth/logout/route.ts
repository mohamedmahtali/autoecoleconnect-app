import { NextResponse } from "next/server";

import { SESSION_COOKIE } from "@/lib/auth";

export async function POST() {
  const reponse = NextResponse.json({ deconnecte: true });
  reponse.cookies.delete(SESSION_COOKIE);
  return reponse;
}
