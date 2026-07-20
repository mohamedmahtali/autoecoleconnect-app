import { useBackend } from "@/lib/useBackend";
import type { ForfaitDto } from "@/types/api";

const formatPrix = new Intl.NumberFormat("fr-FR", { style: "currency", currency: "EUR" });

export default function ForfaitsListe() {
  const { data: forfaits, chargement } = useBackend<ForfaitDto[]>("/api/forfaits");

  return (
    <section>
      <div className="entete-page">
        <h2>Forfaits {forfaits ? `(${forfaits.length})` : ""}</h2>
      </div>
      <div className="tableau-conteneur">
        {chargement ? (
          <p className="vide">Chargement…</p>
        ) : !forfaits || forfaits.length === 0 ? (
          <p className="vide">Aucun forfait pour l’instant.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Nom</th>
                <th>Heures</th>
                <th>Validité</th>
                <th>Prix</th>
                <th>Catégorie</th>
                <th>Kilométrage</th>
              </tr>
            </thead>
            <tbody>
              {forfaits.map((forfait) => (
                <tr key={forfait.id}>
                  <td>{forfait.nom}</td>
                  <td>{forfait.nombreHeure} h</td>
                  <td>
                    {forfait.validite} {forfait.unite === "MOIS" ? "mois" : "jours"}
                  </td>
                  <td>{formatPrix.format(forfait.prix)}</td>
                  <td>{forfait.categorie === "CONDUITE" ? "Conduite" : "Journalier"}</td>
                  <td>
                    {forfait.kilometrage === "ILLIMITE" ? "Illimité" : `${forfait.nbKilometre} km`}
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
