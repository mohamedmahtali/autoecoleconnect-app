import Link from "next/link";

import { fetchBackend } from "@/lib/api";
import type { Identifiable } from "@/types/api";

export const dynamic = "force-dynamic";

export default async function TableauDeBord() {
  const [clients, moniteurs, voitures, forfaits, reservations, seances] =
    await Promise.all([
      fetchBackend<Identifiable[]>("/api/clients"),
      fetchBackend<Identifiable[]>("/api/moniteurs"),
      fetchBackend<Identifiable[]>("/api/voitures"),
      fetchBackend<Identifiable[]>("/api/forfaits"),
      fetchBackend<Identifiable[]>("/api/reservations"),
      fetchBackend<Identifiable[]>("/api/seances"),
    ]);

  const stats = [
    { libelle: "Élèves", valeur: clients.length, lien: "/admin/clients" },
    { libelle: "Moniteurs", valeur: moniteurs.length },
    { libelle: "Véhicules", valeur: voitures.length },
    { libelle: "Forfaits", valeur: forfaits.length },
    { libelle: "Réservations", valeur: reservations.length },
    { libelle: "Séances", valeur: seances.length },
  ];

  return (
    <section className="cartes-stats">
      {stats.map((stat) =>
        stat.lien ? (
          <Link key={stat.libelle} href={stat.lien} className="stat-carte">
            <div className="valeur">{stat.valeur}</div>
            <div className="libelle">{stat.libelle}</div>
          </Link>
        ) : (
          <div key={stat.libelle} className="stat-carte">
            <div className="valeur">{stat.valeur}</div>
            <div className="libelle">{stat.libelle}</div>
          </div>
        ),
      )}
    </section>
  );
}
