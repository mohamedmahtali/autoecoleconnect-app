import { useState } from "react";
import { Link } from "react-router-dom";

import { mutateBackend } from "@/lib/api";
import { useBackend } from "@/lib/useBackend";
import type { ClientDto } from "@/types/api";

const formatDate = new Intl.DateTimeFormat("fr-FR", { dateStyle: "medium" });

export default function ClientsListe() {
  const { data: clients, chargement, recharger } = useBackend<ClientDto[]>("/api/clients");
  const [confirmationPour, setConfirmationPour] = useState<string | null>(null);

  // Droit à l'effacement RGPD : anonymisation irréversible, d'où la
  // confirmation en deux temps plutôt qu'un clic direct.
  async function anonymiser(id: string) {
    await mutateBackend(`/api/clients/${id}/anonymisation`, "POST");
    setConfirmationPour(null);
    recharger();
  }

  return (
    <section>
      <div className="entete-page">
        <h2>Élèves {clients ? `(${clients.length})` : ""}</h2>
        <Link to="/admin/clients/nouveau" className="bouton-secondaire">
          + Nouvel élève
        </Link>
      </div>
      <div className="tableau-conteneur">
        {chargement ? (
          <p className="vide">Chargement…</p>
        ) : !clients || clients.length === 0 ? (
          <p className="vide">Aucun élève pour l’instant.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Nom</th>
                <th>Email</th>
                <th>Téléphone</th>
                <th>Inscrit le</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {clients.map((client) => (
                <tr key={client.id}>
                  <td>
                    {client.prenom} {client.nom}
                  </td>
                  <td>{client.email}</td>
                  <td>{client.telephone ?? "—"}</td>
                  <td>{formatDate.format(new Date(client.createdAt))}</td>
                  <td>
                    {confirmationPour === client.id ? (
                      <div className="actions-ligne">
                        <span className="rgpd-avertissement">
                          Irréversible — données effacées, accès supprimé.
                        </span>
                        <button className="btn-ligne" onClick={() => anonymiser(client.id)}>
                          Confirmer
                        </button>
                        <button className="btn-ligne" onClick={() => setConfirmationPour(null)}>
                          Annuler
                        </button>
                      </div>
                    ) : (
                      <button
                        className="btn-ligne"
                        onClick={() => setConfirmationPour(client.id)}
                      >
                        Anonymiser (RGPD)
                      </button>
                    )}
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
