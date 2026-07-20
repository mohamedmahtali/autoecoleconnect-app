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

export type StatutMoniteur = "PENDING" | "APPROVED" | "REJECTED" | "INACTIVE";

export interface MoniteurDto {
  id: string;
  nom: string;
  prenom: string;
  email: string;
  telephone: string | null;
  statut: StatutMoniteur;
  notes: string | null;
  active: boolean;
  createdAt: string;
}

export interface VoitureDto {
  id: string;
  nom: string;
  marque: string;
  transmission: "MANUELLE" | "AUTOMATIQUE";
  doubleCommande: boolean;
  carburant: string | null;
  couleur: string | null;
  nbPortes: number | null;
  nbPassagers: number | null;
  airConditionne: boolean;
  note: string | null;
  active: boolean;
}

export interface ForfaitDto {
  id: string;
  nom: string;
  nombreHeure: number;
  validite: number;
  unite: "MOIS" | "JOUR";
  prix: number;
  conditions: string | null;
  categorie: "CONDUITE" | "JOURNALIER";
  transmission: "MANUELLE" | "AUTOMATIQUE" | null;
  kilometrage: "ILLIMITE" | "LIMITE";
  nbKilometre: number | null;
  carburant: "INCLUS" | "NON_INCLUS";
  active: boolean;
}

// Pour les compteurs du tableau de bord, seule la présence des éléments compte.
export interface Identifiable {
  id: string;
}

export type StatutSeance = "SCHEDULED" | "COMPLETED" | "CANCELLED" | "NO_SHOW";

export interface SeanceDto {
  id: string;
  reservationId: string;
  clientNomComplet: string;
  moniteurId: string | null;
  moniteurNomComplet: string | null;
  voitureId: string | null;
  voitureNom: string | null;
  dateSeance: string;
  hDeb: string;
  hFin: string;
  statut: StatutSeance;
  validatedClient: boolean;
  validatedMoniteur: boolean;
  validatedAdmin: boolean;
  notes: string | null;
  active: boolean;
  createdAt: string;
}
