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

export type PaiementType =
  | "STRIPE"
  | "PAYPLUG"
  | "ALMA"
  | "ESPECE"
  | "CHEQUE"
  | "VIREMENT"
  | "CPF"
  | "PERMIS1EURO";

export type PaiementStatut = "PENDING" | "PAID" | "REFUNDED" | "FAILED";

export type StatutReservation = "PENDING" | "ACTIVE" | "COMPLETED" | "CANCELLED" | "EXPIRED";

export interface ReservationDto {
  id: string;
  clientId: string;
  clientNomComplet: string;
  forfaitId: string;
  forfaitNom: string;
  dateDebut: string;
  dateFin: string;
  dateReservation: string;
  montant: number;
  paiementType: PaiementType | null;
  paiementStatut: PaiementStatut;
  statut: StatutReservation;
  notes: string | null;
  active: boolean;
}

export type TypeExamen = "CODE" | "CONDUITE";
export type ResultatExamen = "PLANIFIE" | "REUSSI" | "ECHOUE" | "ABSENT";

export interface ExamenDto {
  id: string;
  clientId: string;
  clientNomComplet: string;
  type: TypeExamen;
  dateExamen: string;
  dateConvocation: string | null;
  resultat: ResultatExamen;
  nombreFautes: number | null;
  centreExamen: string | null;
  examinateur: string | null;
  notes: string | null;
  active: boolean;
  createdAt: string;
}

export type JourSemaine =
  | "LUNDI"
  | "MARDI"
  | "MERCREDI"
  | "JEUDI"
  | "VENDREDI"
  | "SAMEDI"
  | "DIMANCHE";

export interface DisponibiliteDto {
  id: string;
  moniteurId: string;
  moniteurNomComplet: string;
  jour: JourSemaine;
  heureDebut: string;
  heureFin: string;
  active: boolean;
  createdAt: string;
}

// Export RGPD « droit d'accès » : ce que l'élève télécharge sur son portail.
export interface DonneesPersonnellesDto {
  identite: ClientDto;
  reservations: ReservationDto[];
  seances: SeanceDto[];
  examens: ExamenDto[];
}

export interface InscriptionMensuelleDto {
  mois: string;
  nombre: number;
}

export interface StatsDto {
  caTotal: number;
  elevesActifs: number;
  seancesTerminees: number;
  seancesNoShow: number;
  tauxNoShow: number;
  examensPresentes: number;
  tauxReussiteExamen: number;
  heuresDispoHebdo: number;
  tauxOccupation: number;
  inscriptionsParMois: InscriptionMensuelleDto[];
}
