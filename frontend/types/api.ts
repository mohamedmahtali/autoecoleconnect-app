// Types de l'API backend.
// Cible (voir docs/03-stack-technique.md) : générés automatiquement depuis la
// spec OpenAPI de Spring Boot (/v3/api-docs). Écrits à la main en Phase 0.

export interface PingResponse {
  status: string;
  service: string;
  timestamp: string;
}

export interface LoginBackendResponse {
  token: string;
  type: string;
  expireLe: string;
  id: string;
  role: "DIRECTEUR" | "MONITEUR" | "CLIENT";
  nomComplet: string;
}

export interface ClientDto {
  id: string;
  nom: string;
  prenom: string;
  email: string;
  telephone: string | null;
  adresse: string | null;
  notes: string | null;
  active: boolean;
  createdAt: string;
}

// Pour les compteurs du tableau de bord, seule la présence des éléments compte.
export interface Identifiable {
  id: string;
}
