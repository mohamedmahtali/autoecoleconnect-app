import { getToken } from "@/lib/session";
import type { PingResponse } from "@/types/api";

// Côté serveur (SSR / route handlers) : BACKEND_URL pointe vers le service
// backend (nom du service docker-compose ou du Service K8s).
export const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";

export async function pingBackend(): Promise<PingResponse | null> {
  try {
    const res = await fetch(`${BACKEND_URL}/api/ping`, { cache: "no-store" });
    if (!res.ok) {
      return null;
    }
    return (await res.json()) as PingResponse;
  } catch {
    return null;
  }
}

// Appel authentifié au backend : le JWT du cookie de session est transmis
// en Authorization. À n'utiliser que depuis des Server Components/handlers.
export async function fetchBackend<T>(chemin: string): Promise<T> {
  const token = await getToken();
  const res = await fetch(`${BACKEND_URL}${chemin}`, {
    cache: "no-store",
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!res.ok) {
    throw new Error(`Appel backend ${chemin} en échec (HTTP ${res.status})`);
  }
  return (await res.json()) as T;
}

export type ResultatMutation =
  | { ok: true }
  | { ok: false; erreur: string };

// Écriture authentifiée (Server Actions) : renvoie le detail du
// ProblemDetail (RFC 9457) du backend en cas d'échec, pour affichage.
export async function mutateBackend(
  chemin: string,
  methode: "POST" | "PUT" | "PATCH" | "DELETE",
  corps?: unknown,
): Promise<ResultatMutation> {
  const token = await getToken();
  const res = await fetch(`${BACKEND_URL}${chemin}`, {
    method: methode,
    cache: "no-store",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: corps !== undefined ? JSON.stringify(corps) : undefined,
  });
  if (res.ok) {
    return { ok: true };
  }
  try {
    const probleme = (await res.json()) as { detail?: string };
    return { ok: false, erreur: probleme.detail ?? `Erreur HTTP ${res.status}` };
  } catch {
    return { ok: false, erreur: `Erreur HTTP ${res.status}` };
  }
}
