import { decoderJwt, sessionValide, type SessionPayload } from "@/lib/auth";

// Sans BFF Next.js, il n'y a plus de cookie httpOnly possible : le JWT vit
// en sessionStorage (effacé à la fermeture de l'onglet, non partagé entre
// onglets — fenêtre d'exposition XSS réduite par rapport à localStorage).
// Mitigations complémentaires : CSP stricte côté nginx (nginx.conf) et
// aucun usage de dangerouslySetInnerHTML dans l'app.
const CLE_TOKEN = "aec_session_token";

export function setToken(token: string): void {
  sessionStorage.setItem(CLE_TOKEN, token);
}

export function getToken(): string | null {
  return sessionStorage.getItem(CLE_TOKEN);
}

export function clearToken(): void {
  sessionStorage.removeItem(CLE_TOKEN);
}

export function getSession(): SessionPayload | null {
  const token = getToken();
  if (!token) {
    return null;
  }
  const session = decoderJwt(token);
  return sessionValide(session) ? session : null;
}
