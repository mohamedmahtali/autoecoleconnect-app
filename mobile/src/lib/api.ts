import { getEcoleSlug, getToken } from "./session";
import type { LoginBackendResponse } from "../types/api";

// Contrairement au web (une origine par tenant, /api relatif suffit), une
// même appli installée sert tous les tenants : l'utilisateur indique son
// auto-école (sous-domaine) une fois, mémorisé ensuite dans SecureStore.
function urlEcole(slug: string): string {
  return `https://${slug}.autoecoleconnect.fr`;
}

async function baseUrl(): Promise<string> {
  const slug = await getEcoleSlug();
  if (!slug) {
    throw new Error("Aucune auto-école sélectionnée");
  }
  return urlEcole(slug);
}

export async function login(
  ecoleSlug: string,
  email: string,
  motDePasse: string,
): Promise<LoginBackendResponse> {
  const res = await fetch(`${urlEcole(ecoleSlug)}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, motDePasse }),
  });
  const corps = await res.json();
  if (!res.ok) {
    throw new Error(corps.detail ?? "Connexion impossible");
  }
  return corps as LoginBackendResponse;
}

export async function fetchBackend<T>(chemin: string): Promise<T> {
  const [url, token] = await Promise.all([baseUrl(), getToken()]);
  const res = await fetch(`${url}${chemin}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!res.ok) {
    throw new Error(`Appel backend ${chemin} en échec (HTTP ${res.status})`);
  }
  return (await res.json()) as T;
}

export type ResultatMutation = { ok: true } | { ok: false; erreur: string };

export async function mutateBackend(
  chemin: string,
  methode: "POST" | "PUT" | "PATCH" | "DELETE",
  corps?: unknown,
): Promise<ResultatMutation> {
  const [url, token] = await Promise.all([baseUrl(), getToken()]);
  const res = await fetch(`${url}${chemin}`, {
    method: methode,
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
