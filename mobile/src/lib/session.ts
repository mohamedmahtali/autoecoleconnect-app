import * as SecureStore from "expo-secure-store";

// Contrairement au web (sessionStorage, effacé à la fermeture de l'onglet),
// une appli installée doit survivre aux redémarrages : le JWT vit dans le
// Keychain (iOS) / Keystore (Android) via expo-secure-store — stockage
// chiffré au niveau matériel, jamais accessible à du JS injecté (pas de
// surface XSS possible dans une app native comme dans un navigateur).
const CLE_TOKEN = "aec_token";
const CLE_ECOLE = "aec_ecole_slug";

export interface SessionPayload {
  sub: string;
  email: string;
  role: "DIRECTEUR" | "MONITEUR" | "CLIENT";
  nomComplet: string;
  exp: number; // secondes epoch
}

// Décode le payload du JWT sans vérifier la signature : la vérification
// cryptographique est faite par le backend à chaque appel API.
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

export async function setSession(ecoleSlug: string, token: string): Promise<void> {
  await SecureStore.setItemAsync(CLE_ECOLE, ecoleSlug);
  await SecureStore.setItemAsync(CLE_TOKEN, token);
}

export async function getEcoleSlug(): Promise<string | null> {
  return SecureStore.getItemAsync(CLE_ECOLE);
}

export async function getToken(): Promise<string | null> {
  return SecureStore.getItemAsync(CLE_TOKEN);
}

export async function clearToken(): Promise<void> {
  await SecureStore.deleteItemAsync(CLE_TOKEN);
  // Le slug de l'école est conservé : évite de le retaper à la prochaine
  // connexion (un moniteur ne change pas d'auto-école d'une session à l'autre).
}

export async function getSession(): Promise<SessionPayload | null> {
  const token = await getToken();
  if (!token) {
    return null;
  }
  const session = decoderJwt(token);
  return sessionValide(session) ? session : null;
}
