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
