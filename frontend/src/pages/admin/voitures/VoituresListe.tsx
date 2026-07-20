import { useBackend } from "@/lib/useBackend";
import type { VoitureDto } from "@/types/api";

export default function VoituresListe() {
  const { data: voitures, chargement } = useBackend<VoitureDto[]>("/api/voitures");

  return (
    <section>
      <div className="entete-page">
        <h2>Véhicules {voitures ? `(${voitures.length})` : ""}</h2>
      </div>
      <div className="tableau-conteneur">
        {chargement ? (
          <p className="vide">Chargement…</p>
        ) : !voitures || voitures.length === 0 ? (
          <p className="vide">Aucun véhicule pour l’instant.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Nom</th>
                <th>Marque</th>
                <th>Transmission</th>
                <th>Double commande</th>
                <th>Couleur</th>
                <th>Clim</th>
              </tr>
            </thead>
            <tbody>
              {voitures.map((voiture) => (
                <tr key={voiture.id}>
                  <td>{voiture.nom}</td>
                  <td>{voiture.marque}</td>
                  <td>{voiture.transmission === "MANUELLE" ? "Manuelle" : "Automatique"}</td>
                  <td>{voiture.doubleCommande ? "Oui" : "Non"}</td>
                  <td>{voiture.couleur ?? "—"}</td>
                  <td>{voiture.airConditionne ? "Oui" : "Non"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </section>
  );
}
