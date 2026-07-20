// Gestion du JWT — module sans dépendance framework, portable tel quel
// (c'était déjà vrai côté Next.js : importé par le middleware Edge).

export const SESSION_COOKIE = "aec_session";

export interface SessionPayload {
  sub: string;
  email: string;
  role: "DIRECTEUR" | "MONITEUR" | "CLIENT";
  nomComplet: string;
  exp: number; // secondes epoch
}

// Décode le payload du JWT sans vérifier la signature : la vérification
// cryptographique est faite par le backend à chaque appel API. Ici on ne
// lit que des informations d'affichage et d'aiguillage (rôle, expiration).
export function decoderJwt(token: string): SessionPayload | null {
  try {
    const base64 = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    const octets = Uint8Array.from(atob(base64), (c) => c.charCodeAt(0));
    return JSON.parse(new TextDecoder().decode(octets)) as SessionPayload;
  } catch {
    return null;
  }
}

export function sessionValide(session: SessionPayload | null): session is SessionPayload {
  return session !== null && session.exp * 1000 > Date.now();
}
