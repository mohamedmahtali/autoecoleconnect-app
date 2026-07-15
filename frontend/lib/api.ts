import type { PingResponse } from "@/types/api";

// Côté serveur (SSR / route handlers) : BACKEND_URL pointe vers le service
// backend (nom du service docker-compose ou du Service K8s).
const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";

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
