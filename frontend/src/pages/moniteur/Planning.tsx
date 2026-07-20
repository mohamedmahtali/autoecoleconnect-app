import { mutateBackend } from "@/lib/api";
import { useBackend } from "@/lib/useBackend";
import type { SeanceDto } from "@/types/api";

const LIBELLES_STATUT: Record<SeanceDto["statut"], string> = {
  SCHEDULED: "Planifiée",
  COMPLETED: "Terminée",
  CANCELLED: "Annulée",
  NO_SHOW: "Absence",
};

// Réutilise la même palette sémantique que les badges de statut moniteur
// (globals.css) plutôt que d'en inventer une nouvelle par type d'objet.
const BADGE_CLASSE: Record<SeanceDto["statut"], string> = {
  SCHEDULED: "badge-pending",
  COMPLETED: "badge-approved",
  CANCELLED: "badge-inactive",
  NO_SHOW: "badge-rejected",
};

function formaterHeure(heure: string) {
  return heure.slice(0, 5);
}

function formaterDate(date: string) {
  return new Date(`${date}T00:00:00`).toLocaleDateString("fr-FR", {
    weekday: "long",
    day: "numeric",
    month: "long",
  });
}

export default function Planning() {
  const { data: seances, chargement, recharger } = useBackend<SeanceDto[]>("/api/seances");

  async function confirmer(id: string) {
    await mutateBackend(`/api/seances/${id}/validation-moniteur`, "PATCH");
    recharger();
  }

  if (chargement) {
    return <p className="vide">Chargement…</p>;
  }
  if (!seances || seances.length === 0) {
    return <p className="vide">Aucune séance planifiée pour l’instant.</p>;
  }

  const triees = [...seances].sort((a, b) =>
    `${a.dateSeance}${a.hDeb}`.localeCompare(`${b.dateSeance}${b.hDeb}`),
  );

  return (
    <section className="liste-seances">
      {triees.map((seance) => (
        <article key={seance.id} className="seance-carte">
          <div className="seance-entete">
            <span className="seance-date">{formaterDate(seance.dateSeance)}</span>
            <span className={`badge ${BADGE_CLASSE[seance.statut]}`}>
              {LIBELLES_STATUT[seance.statut]}
            </span>
          </div>
          <div className="seance-corps">
            <span className="seance-horaire">
              {formaterHeure(seance.hDeb)} – {formaterHeure(seance.hFin)}
            </span>
            <span>{seance.clientNomComplet}</span>
            {seance.voitureNom && <span>{seance.voitureNom}</span>}
          </div>
          {seance.statut === "SCHEDULED" && (
            <div className="seance-actions">
              {seance.validatedMoniteur ? (
                <span className="ok">✓ Présence confirmée</span>
              ) : (
                <button className="bouton" onClick={() => confirmer(seance.id)}>
                  Confirmer ma présence
                </button>
              )}
            </div>
          )}
        </article>
      ))}
    </section>
  );
}
