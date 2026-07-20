import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";

import { getSession } from "@/lib/session";
import type { SessionPayload } from "@/lib/auth";

// Garde d'accès côté client — reproduit la logique de l'ancien middleware.ts
// Next.js (session présente, non expirée, bon rôle). La protection réelle
// reste côté backend : le JWT est vérifié cryptographiquement à chaque appel
// API, quoi qu'il arrive ici.
export function RequireAuth({
  role,
  children,
}: {
  role?: SessionPayload["role"];
  children: ReactNode;
}) {
  const location = useLocation();
  const session = getSession();

  if (!session) {
    const redirige = encodeURIComponent(location.pathname);
    return <Navigate to={`/login?redirige=${redirige}`} replace />;
  }
  if (role && session.role !== role) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}
