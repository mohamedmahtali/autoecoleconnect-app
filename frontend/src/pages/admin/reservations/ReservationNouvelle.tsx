import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";

import { mutateBackend } from "@/lib/api";
import { useBackend } from "@/lib/useBackend";
import type { ClientDto, ForfaitDto } from "@/types/api";

const eur = new Intl.NumberFormat("fr-FR", { style: "currency", currency: "EUR" });

export default function ReservationNouvelle() {
  const navigate = useNavigate();
  const { data: clients } = useBackend<ClientDto[]>("/api/clients");
  const { data: forfaits } = useBackend<ForfaitDto[]>("/api/forfaits");
  const [erreur, setErreur] = useState<string | null>(null);
  const [enCours, setEnCours] = useState(false);

  async function creer(evenement: FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    setErreur(null);
    setEnCours(true);
    const donnees = new FormData(evenement.currentTarget);
    const montant = donnees.get("montant") as string;
    // dateFin et, si laissé vide, montant sont déduits du forfait côté backend
    // (ReservationCreationRequest) — on n'envoie le montant que s'il est saisi.
    const resultat = await mutateBackend("/api/reservations", "POST", {
      clientId: donnees.get("clientId"),
      forfaitId: donnees.get("forfaitId"),
      dateDebut: donnees.get("dateDebut"),
      montant: montant ? Number(montant) : null,
      notes: donnees.get("notes") || null,
    });
    setEnCours(false);
    if (!resultat.ok) {
      setErreur(resultat.erreur);
      return;
    }
    navigate("/admin/reservations");
  }

  return (
    <section>
      <div className="entete-page">
        <h2>Nouvelle réservation</h2>
        <Link to="/admin/reservations">← Retour à la liste</Link>
      </div>
      <form className="carte" onSubmit={creer} style={{ maxWidth: "28rem" }}>
        {erreur && <p className="erreur">{erreur}</p>}
        <label className="champ">
          <span>Élève</span>
          <select name="clientId" required defaultValue="">
            <option value="" disabled>
              Choisir un élève…
            </option>
            {(clients ?? []).map((c) => (
              <option key={c.id} value={c.id}>
                {c.prenom} {c.nom}
              </option>
            ))}
          </select>
        </label>
        <label className="champ">
          <span>Forfait</span>
          <select name="forfaitId" required defaultValue="">
            <option value="" disabled>
              Choisir un forfait…
            </option>
            {(forfaits ?? []).map((f) => (
              <option key={f.id} value={f.id}>
                {f.nom} — {eur.format(f.prix)}
              </option>
            ))}
          </select>
        </label>
        <label className="champ">
          <span>Date de début</span>
          <input name="dateDebut" type="date" required />
        </label>
        <label className="champ">
          <span>Montant (optionnel — par défaut le prix du forfait)</span>
          <input name="montant" type="number" min="0" step="0.01" />
        </label>
        <label className="champ">
          <span>Notes (optionnel)</span>
          <input name="notes" />
        </label>
        <button className="bouton" type="submit" disabled={enCours}>
          {enCours ? "Création…" : "Créer la réservation"}
        </button>
      </form>
    </section>
  );
}
