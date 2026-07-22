import { Fragment, useState } from "react";
import { Link } from "react-router-dom";

import { mutateBackend } from "@/lib/api";
import { useBackend } from "@/lib/useBackend";
import type {
  PaiementStatut,
  PaiementType,
  ReservationDto,
  StatutReservation,
} from "@/types/api";

const eur = new Intl.NumberFormat("fr-FR", { style: "currency", currency: "EUR" });
const formatDate = new Intl.DateTimeFormat("fr-FR", { dateStyle: "medium" });

// Moyens encaissés « à la main » par le directeur — les fournisseurs en ligne
// (Stripe/Payplug/Alma) sont exclus : ils ne se saisissent pas manuellement.
const MOYENS_MANUELS: { valeur: PaiementType; libelle: string }[] = [
  { valeur: "ESPECE", libelle: "Espèces" },
  { valeur: "CHEQUE", libelle: "Chèque" },
  { valeur: "VIREMENT", libelle: "Virement" },
  { valeur: "CPF", libelle: "CPF" },
  { valeur: "PERMIS1EURO", libelle: "Permis à 1 €" },
];

const LIBELLE_STATUT: Record<StatutReservation, string> = {
  PENDING: "En attente",
  ACTIVE: "Active",
  COMPLETED: "Terminée",
  CANCELLED: "Annulée",
  EXPIRED: "Expirée",
};

const LIBELLE_PAIEMENT: Record<PaiementStatut, string> = {
  PENDING: "À encaisser",
  PAID: "Payé",
  REFUNDED: "Remboursé",
  FAILED: "Échoué",
};

// Réutilise les 4 variantes de badge existantes (pending/approved/rejected/
// inactive) plutôt que d'en inventer de nouvelles.
const BADGE_STATUT: Record<StatutReservation, string> = {
  PENDING: "pending",
  ACTIVE: "approved",
  COMPLETED: "approved",
  CANCELLED: "rejected",
  EXPIRED: "inactive",
};

const BADGE_PAIEMENT: Record<PaiementStatut, string> = {
  PENDING: "pending",
  PAID: "approved",
  REFUNDED: "inactive",
  FAILED: "rejected",
};

export default function ReservationsListe() {
  const { data: reservations, chargement, recharger } =
    useBackend<ReservationDto[]>("/api/reservations");
  const [paiementPour, setPaiementPour] = useState<string | null>(null);
  const [moyen, setMoyen] = useState<PaiementType>("ESPECE");
  const [reference, setReference] = useState("");
  const [erreur, setErreur] = useState<string | null>(null);
  const [enCours, setEnCours] = useState(false);

  function ouvrirPaiement(id: string) {
    setPaiementPour(id);
    setMoyen("ESPECE");
    setReference("");
    setErreur(null);
  }

  async function enregistrerPaiement(id: string) {
    setEnCours(true);
    setErreur(null);
    const resultat = await mutateBackend(`/api/reservations/${id}/paiement`, "PATCH", {
      paiementType: moyen,
      reference: reference || null,
    });
    setEnCours(false);
    if (!resultat.ok) {
      setErreur(resultat.erreur);
      return;
    }
    setPaiementPour(null);
    recharger();
  }

  async function annuler(id: string) {
    // Le backend refuse d'annuler une réservation qui n'est ni PENDING ni
    // ACTIVE ; l'UI ne propose le bouton que dans ces cas, on recharge.
    await mutateBackend(`/api/reservations/${id}/annulation`, "POST");
    recharger();
  }

  return (
    <section>
      <div className="entete-page">
        <h2>Réservations {reservations ? `(${reservations.length})` : ""}</h2>
        <Link to="/admin/reservations/nouvelle" className="bouton-secondaire">
          + Nouvelle réservation
        </Link>
      </div>
      <div className="tableau-conteneur">
        {chargement ? (
          <p className="vide">Chargement…</p>
        ) : !reservations || reservations.length === 0 ? (
          <p className="vide">Aucune réservation pour l’instant.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Élève</th>
                <th>Forfait</th>
                <th>Période</th>
                <th>Montant</th>
                <th>Statut</th>
                <th>Paiement</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {reservations.map((r) => (
                <Fragment key={r.id}>
                  <tr>
                    <td>{r.clientNomComplet}</td>
                    <td>{r.forfaitNom}</td>
                    <td>
                      {formatDate.format(new Date(r.dateDebut))} →{" "}
                      {formatDate.format(new Date(r.dateFin))}
                    </td>
                    <td>{eur.format(r.montant)}</td>
                    <td>
                      <span className={`badge badge-${BADGE_STATUT[r.statut]}`}>
                        {LIBELLE_STATUT[r.statut]}
                      </span>
                    </td>
                    <td>
                      <span className={`badge badge-${BADGE_PAIEMENT[r.paiementStatut]}`}>
                        {LIBELLE_PAIEMENT[r.paiementStatut]}
                      </span>
                    </td>
                    <td>
                      <div className="actions-ligne">
                        {r.paiementStatut === "PENDING" && (
                          <button className="btn-ligne" onClick={() => ouvrirPaiement(r.id)}>
                            Encaisser
                          </button>
                        )}
                        {(r.statut === "PENDING" || r.statut === "ACTIVE") && (
                          <button className="btn-ligne" onClick={() => annuler(r.id)}>
                            Annuler
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                  {paiementPour === r.id && (
                    <tr>
                      <td colSpan={7}>
                        <div className="paiement-form">
                          {erreur && <p className="erreur">{erreur}</p>}
                          <label className="champ">
                            <span>Moyen de paiement</span>
                            <select
                              value={moyen}
                              onChange={(e) => setMoyen(e.target.value as PaiementType)}
                            >
                              {MOYENS_MANUELS.map((m) => (
                                <option key={m.valeur} value={m.valeur}>
                                  {m.libelle}
                                </option>
                              ))}
                            </select>
                          </label>
                          <label className="champ">
                            <span>Référence (n° de chèque, virement… — optionnel)</span>
                            <input
                              value={reference}
                              onChange={(e) => setReference(e.target.value)}
                            />
                          </label>
                          <div className="actions-ligne">
                            <button
                              className="btn-ligne"
                              disabled={enCours}
                              onClick={() => enregistrerPaiement(r.id)}
                            >
                              {enCours ? "Enregistrement…" : "Valider le paiement"}
                            </button>
                            <button className="btn-ligne" onClick={() => setPaiementPour(null)}>
                              Fermer
                            </button>
                          </div>
                        </div>
                      </td>
                    </tr>
                  )}
                </Fragment>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </section>
  );
}
