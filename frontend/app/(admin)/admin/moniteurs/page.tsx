import { fetchBackend } from "@/lib/api";
import type { MoniteurDto, StatutMoniteur } from "@/types/api";
import { changerStatutMoniteur } from "./actions";

export const dynamic = "force-dynamic";

const LIBELLES: Record<StatutMoniteur, string> = {
  PENDING: "En attente",
  APPROVED: "Approuvé",
  REJECTED: "Rejeté",
  INACTIVE: "Inactif",
};

function BoutonTransition({
  id,
  cible,
  libelle,
}: {
  id: string;
  cible: StatutMoniteur;
  libelle: string;
}) {
  return (
    <form action={changerStatutMoniteur.bind(null, id, cible)}>
      <button className="btn-ligne" type="submit">
        {libelle}
      </button>
    </form>
  );
}

export default async function PageMoniteurs() {
  const moniteurs = await fetchBackend<MoniteurDto[]>("/api/moniteurs");

  return (
    <section>
      <div className="entete-page">
        <h2>Moniteurs ({moniteurs.length})</h2>
      </div>
      <div className="tableau-conteneur">
        {moniteurs.length === 0 ? (
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
                          <BoutonTransition id={moniteur.id} cible="APPROVED" libelle="Approuver" />
                          <BoutonTransition id={moniteur.id} cible="REJECTED" libelle="Rejeter" />
                        </>
                      )}
                      {moniteur.statut === "APPROVED" && (
                        <BoutonTransition id={moniteur.id} cible="INACTIVE" libelle="Désactiver" />
                      )}
                      {moniteur.statut === "INACTIVE" && (
                        <BoutonTransition id={moniteur.id} cible="APPROVED" libelle="Réactiver" />
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
