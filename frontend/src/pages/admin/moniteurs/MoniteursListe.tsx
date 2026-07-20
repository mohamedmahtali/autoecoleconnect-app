import { mutateBackend } from "@/lib/api";
import { useBackend } from "@/lib/useBackend";
import type { MoniteurDto, StatutMoniteur } from "@/types/api";

const LIBELLES: Record<StatutMoniteur, string> = {
  PENDING: "En attente",
  APPROVED: "Approuvé",
  REJECTED: "Rejeté",
  INACTIVE: "Inactif",
};

export default function MoniteursListe() {
  const { data: moniteurs, chargement, recharger } = useBackend<MoniteurDto[]>("/api/moniteurs");

  async function changerStatut(id: string, statut: StatutMoniteur) {
    // Les transitions interdites sont refusées par le backend (400) ; l'UI ne
    // proposant que les boutons valides, on se contente de recharger.
    await mutateBackend(`/api/moniteurs/${id}/statut`, "PATCH", { statut });
    recharger();
  }

  return (
    <section>
      <div className="entete-page">
        <h2>Moniteurs {moniteurs ? `(${moniteurs.length})` : ""}</h2>
      </div>
      <div className="tableau-conteneur">
        {chargement ? (
          <p className="vide">Chargement…</p>
        ) : !moniteurs || moniteurs.length === 0 ? (
          <p className="vide">Aucun moniteur pour l’instant.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Nom</th>
                <th>Email</th>
                <th>Téléphone</th>
                <th>Statut</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {moniteurs.map((moniteur) => (
                <tr key={moniteur.id}>
                  <td>
                    {moniteur.prenom} {moniteur.nom}
                  </td>
                  <td>{moniteur.email}</td>
                  <td>{moniteur.telephone ?? "—"}</td>
                  <td>
                    <span className={`badge badge-${moniteur.statut.toLowerCase()}`}>
                      {LIBELLES[moniteur.statut]}
                    </span>
                  </td>
                  <td>
                    <div className="actions-ligne">
                      {moniteur.statut === "PENDING" && (
                        <>
                          <button className="btn-ligne" onClick={() => changerStatut(moniteur.id, "APPROVED")}>
                            Approuver
                          </button>
                          <button className="btn-ligne" onClick={() => changerStatut(moniteur.id, "REJECTED")}>
                            Rejeter
                          </button>
                        </>
                      )}
                      {moniteur.statut === "APPROVED" && (
                        <button className="btn-ligne" onClick={() => changerStatut(moniteur.id, "INACTIVE")}>
                          Désactiver
                        </button>
                      )}
                      {moniteur.statut === "INACTIVE" && (
                        <button className="btn-ligne" onClick={() => changerStatut(moniteur.id, "APPROVED")}>
                          Réactiver
                        </button>
                      )}
                    </div>
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
