// Mêmes formes que frontend/src/types/api.ts (backend partagé) — pas de
// dépendance entre les deux projets, juste le même contrat HTTP à jour.

export interface LoginBackendResponse {
  token: string;
  type: string;
  expireLe: string;
  id: string;
  role: "DIRECTEUR" | "MONITEUR" | "CLIENT";
  nomComplet: string;
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
