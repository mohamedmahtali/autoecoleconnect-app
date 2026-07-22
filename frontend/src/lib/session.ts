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

/**
 * Récupère un jeton transmis par le control-plane quand le gérant ouvre une
 * de ses agences depuis son tableau de bord (docs/18 §18.3 lot 5).
 *
 * Le jeton arrive dans le **fragment** de l'URL (`#acces=…`) et non dans la
 * query string : un fragment n'est jamais envoyé au serveur, il n'apparaît
 * donc ni dans les logs d'accès ni dans l'en-tête Referer. Il est retiré de
 * l'adresse immédiatement après lecture, pour qu'un partage de lien ou une
 * capture d'écran ne l'emporte pas avec lui.
 *
 * ⚠️ Reste l'historique local du navigateur, que `replaceState` ne purge pas
 * rétroactivement. C'est le compromis assumé : entre deux domaines
 * distincts, il n'existe pas de canal plus discret sans ajouter un aller-retour
 * serveur et un jeton à usage unique à stocker.
 */
export function consommerJetonDeLUrl(): boolean {
  const fragment = window.location.hash;
  if (!fragment.startsWith("#acces=")) {
    return false;
  }
  const token = decodeURIComponent(fragment.slice("#acces=".length));
  if (!token) {
    return false;
  }
  setToken(token);
  window.history.replaceState(null, "", window.location.pathname + window.location.search);
  return true;
}
