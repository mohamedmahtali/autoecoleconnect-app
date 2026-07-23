import { useState, type FormEvent } from "react";

import { mutateBackend } from "@/lib/api";
import { useBackend } from "@/lib/useBackend";
import type { DisponibiliteDto, JourSemaine, MoniteurDto } from "@/types/api";

const JOURS: { valeur: JourSemaine; libelle: string }[] = [
  { valeur: "LUNDI", libelle: "Lundi" },
  { valeur: "MARDI", libelle: "Mardi" },
  { valeur: "MERCREDI", libelle: "Mercredi" },
  { valeur: "JEUDI", libelle: "Jeudi" },
  { valeur: "VENDREDI", libelle: "Vendredi" },
  { valeur: "SAMEDI", libelle: "Samedi" },
  { valeur: "DIMANCHE", libelle: "Dimanche" },
];

const ORDRE_JOUR: Record<JourSemaine, number> = {
  LUNDI: 0,
  MARDI: 1,
  MERCREDI: 2,
  JEUDI: 3,
  VENDREDI: 4,
  SAMEDI: 5,
  DIMANCHE: 6,
};

const LIBELLE_JOUR = Object.fromEntries(JOURS.map((j) => [j.valeur, j.libelle])) as Record<
  JourSemaine,
  string
>;

// "09:00:00" (LocalTime sérialisé) ou "09:00" -> "09:00"
const hhmm = (heure: string) => heure.slice(0, 5);

export default function DisponibilitesListe() {
  const { data: dispos, chargement, recharger } =
    useBackend<DisponibiliteDto[]>("/api/disponibilites");
  const { data: moniteurs } = useBackend<MoniteurDto[]>("/api/moniteurs");
  const [erreur, setErreur] = useState<string | null>(null);
  const [enCours, setEnCours] = useState(false);

  async function ajouter(evenement: FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    const form = evenement.currentTarget; // capturé avant l'await (React réinitialise currentTarget après)
    setErreur(null);
    setEnCours(true);
    const d = new FormData(form);
    const resultat = await mutateBackend("/api/disponibilites", "POST", {
      moniteurId: d.get("moniteurId"),
      jour: d.get("jour"),
      heureDebut: d.get("heureDebut"),
      heureFin: d.get("heureFin"),
    });
    setEnCours(false);
    if (!resultat.ok) {
      setErreur(resultat.erreur);
      return;
    }
    form.reset();
    recharger();
  }

  async function supprimer(id: string) {
    await mutateBackend(`/api/disponibilites/${id}`, "DELETE");
    recharger();
  }

  const triees = [...(dispos ?? [])].sort(
    (a, b) =>
      a.moniteurNomComplet.localeCompare(b.moniteurNomComplet) ||
      ORDRE_JOUR[a.jour] - ORDRE_JOUR[b.jour] ||
      a.heureDebut.localeCompare(b.heureDebut),
  );

  return (
    <section>
      <div className="entete-page">
        <h2>Disponibilités moniteur {dispos ? `(${dispos.length})` : ""}</h2>
      </div>

      <form className="carte" onSubmit={ajouter} style={{ maxWidth: "32rem", marginBottom: "1.5rem" }}>
        {erreur && <p className="erreur">{erreur}</p>}
        <label className="champ">
          <span>Moniteur</span>
          <select name="moniteurId" required defaultValue="">
            <option value="" disabled>
              Choisir un moniteur…
            </option>
            {(moniteurs ?? []).map((m) => (
              <option key={m.id} value={m.id}>
                {m.prenom} {m.nom}
              </option>
            ))}
          </select>
        </label>
        <label className="champ">
          <span>Jour</span>
          <select name="jour" defaultValue="LUNDI">
            {JOURS.map((j) => (
              <option key={j.valeur} value={j.valeur}>
                {j.libelle}
              </option>
            ))}
          </select>
        </label>
        <label className="champ">
          <span>Heure de début</span>
          <input name="heureDebut" type="time" required defaultValue="09:00" />
        </label>
        <label className="champ">
          <span>Heure de fin</span>
          <input name="heureFin" type="time" required defaultValue="12:00" />
        </label>
        <button className="bouton" type="submit" disabled={enCours}>
          {enCours ? "Ajout…" : "Ajouter le créneau"}
        </button>
      </form>

      <div className="tableau-conteneur">
        {chargement ? (
          <p className="vide">Chargement…</p>
        ) : triees.length === 0 ? (
          <p className="vide">Aucun créneau de disponibilité pour l’instant.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Moniteur</th>
                <th>Jour</th>
                <th>Horaire</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {triees.map((creneau) => (
                <tr key={creneau.id}>
                  <td>{creneau.moniteurNomComplet}</td>
                  <td>{LIBELLE_JOUR[creneau.jour]}</td>
                  <td>
                    {hhmm(creneau.heureDebut)} – {hhmm(creneau.heureFin)}
                  </td>
                  <td>
                    <button className="btn-ligne" onClick={() => supprimer(creneau.id)}>
                      Supprimer
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </section>
  );
}
