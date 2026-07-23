import { Link } from "react-router-dom";

import { useBackend } from "@/lib/useBackend";
import type { ExamenDto, ResultatExamen, TypeExamen } from "@/types/api";

const formatDate = new Intl.DateTimeFormat("fr-FR", { dateStyle: "medium" });

const LIBELLE_TYPE: Record<TypeExamen, string> = { CODE: "Code", CONDUITE: "Conduite" };

const LIBELLE_RESULTAT: Record<ResultatExamen, string> = {
  PLANIFIE: "Planifié",
  REUSSI: "Réussi",
  ECHOUE: "Échoué",
  ABSENT: "Absent",
};

// Réutilise les 4 variantes de badge existantes.
const BADGE_RESULTAT: Record<ResultatExamen, string> = {
  PLANIFIE: "pending",
  REUSSI: "approved",
  ECHOUE: "rejected",
  ABSENT: "inactive",
};

export default function ExamensListe() {
  const { data: examens, chargement } = useBackend<ExamenDto[]>("/api/examens");

  return (
    <section>
      <div className="entete-page">
        <h2>Examens {examens ? `(${examens.length})` : ""}</h2>
        <Link to="/admin/examens/nouveau" className="bouton-secondaire">
          + Nouvel examen
        </Link>
      </div>
      <div className="tableau-conteneur">
        {chargement ? (
          <p className="vide">Chargement…</p>
        ) : !examens || examens.length === 0 ? (
          <p className="vide">Aucun examen enregistré.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Élève</th>
                <th>Type</th>
                <th>Date</th>
                <th>Résultat</th>
                <th>Fautes</th>
                <th>Centre</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {examens.map((examen) => (
                <tr key={examen.id}>
                  <td>{examen.clientNomComplet}</td>
                  <td>{LIBELLE_TYPE[examen.type]}</td>
                  <td>{formatDate.format(new Date(examen.dateExamen))}</td>
                  <td>
                    <span className={`badge badge-${BADGE_RESULTAT[examen.resultat]}`}>
                      {LIBELLE_RESULTAT[examen.resultat]}
                    </span>
                  </td>
                  <td>{examen.nombreFautes ?? "—"}</td>
                  <td>{examen.centreExamen ?? "—"}</td>
                  <td>
                    <Link to={`/admin/examens/${examen.id}`} className="btn-ligne">
                      Modifier
                    </Link>
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
