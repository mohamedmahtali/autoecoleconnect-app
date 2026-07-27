// Types de l'API backend — SOURCE UNIQUE DE VÉRITÉ : la spec OpenAPI du backend.
//
// Ce fichier n'est qu'un ADAPTATEUR : il ré-exporte, sous leurs noms usuels, les
// types de `api.generated.ts` (généré par `npm run gen:types` depuis
// `openapi.json`, lui-même produit par le backend — voir docs/16 #29). Ne pas
// éditer à la main la FORME des types : modifier le DTO backend, puis régénérer
// (`npm run gen:types`). Le check CI échoue si les types committés ne
// correspondent plus au backend → zéro dérive silencieuse.
import type { components } from "./api.generated";

type Schemas = components["schemas"];

// --- Objets (réponses API) ---
export type LoginBackendResponse = Schemas["LoginResponse"];
export type ClientDto = Schemas["ClientResponse"];
export type MoniteurDto = Schemas["MoniteurResponse"];
export type VoitureDto = Schemas["VoitureResponse"];
export type ForfaitDto = Schemas["ForfaitResponse"];
export type SeanceDto = Schemas["SeanceResponse"];
export type ReservationDto = Schemas["ReservationResponse"];
export type ExamenDto = Schemas["ExamenResponse"];
export type DisponibiliteDto = Schemas["DisponibiliteResponse"];
export type DonneesPersonnellesDto = Schemas["DonneesPersonnellesResponse"];
export type InscriptionMensuelleDto = Schemas["InscriptionMensuelle"];
export type StatsDto = Schemas["StatsResponse"];

// --- Énumérations (émises inline dans les schémas → extraites de la propriété) ---
export type StatutMoniteur = Schemas["MoniteurResponse"]["statut"];
export type StatutSeance = Schemas["SeanceResponse"]["statut"];
export type PaiementType = NonNullable<Schemas["ReservationResponse"]["paiementType"]>;
export type PaiementStatut = Schemas["ReservationResponse"]["paiementStatut"];
export type StatutReservation = Schemas["ReservationResponse"]["statut"];
export type TypeExamen = Schemas["ExamenResponse"]["type"];
export type ResultatExamen = Schemas["ExamenResponse"]["resultat"];
export type JourSemaine = Schemas["DisponibiliteResponse"]["jour"];

// --- Types purement frontend (aucun schéma backend correspondant) ---
// Ping renvoie un Map côté backend (pas de DTO), donc pas de schéma généré.
export interface PingResponse {
  status: string;
  service: string;
  timestamp: string;
}

// Pour les compteurs du tableau de bord, seule la présence des éléments compte.
export interface Identifiable {
  id: string;
}
