import Link from "next/link";

import { fetchBackend } from "@/lib/api";
import type { ClientDto } from "@/types/api";

export const dynamic = "force-dynamic";

const formatDate = new Intl.DateTimeFormat("fr-FR", {
  dateStyle: "medium",
});

export default async function PageClients() {
  const clients = await fetchBackend<ClientDto[]>("/api/clients");

  return (
    <section>
      <div className="entete-page">
        <h2>Élèves ({clients.length})</h2>
        <Link href="/admin/clients/nouveau" className="bouton-secondaire">
          + Nouvel élève
        </Link>
      </div>
      <div className="tableau-conteneur">
        {clients.length === 0 ? (
          <p className="vide">Aucun élève pour l’instant.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Nom</th>
                <th>Email</th>
                <th>Téléphone</th>
                <th>Inscrit le</th>
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
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </section>
  );
}
