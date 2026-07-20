import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

import { fetchBackend } from "@/lib/api";
import type { Identifiable } from "@/types/api";

interface Stat {
  libelle: string;
  valeur: number;
  lien?: string;
}

export default function Dashboard() {
  const [stats, setStats] = useState<Stat[] | null>(null);

  useEffect(() => {
    Promise.all([
      fetchBackend<Identifiable[]>("/api/clients"),
      fetchBackend<Identifiable[]>("/api/moniteurs"),
      fetchBackend<Identifiable[]>("/api/voitures"),
      fetchBackend<Identifiable[]>("/api/forfaits"),
      fetchBackend<Identifiable[]>("/api/reservations"),
      fetchBackend<Identifiable[]>("/api/seances"),
    ]).then(([clients, moniteurs, voitures, forfaits, reservations, seances]) => {
      setStats([
        { libelle: "Élèves", valeur: clients.length, lien: "/admin/clients" },
        { libelle: "Moniteurs", valeur: moniteurs.length },
        { libelle: "Véhicules", valeur: voitures.length },
        { libelle: "Forfaits", valeur: forfaits.length },
        { libelle: "Réservations", valeur: reservations.length },
        { libelle: "Séances", valeur: seances.length },
      ]);
    });
  }, []);

  if (!stats) {
    return <p className="vide">Chargement…</p>;
  }

  return (
    <section className="cartes-stats">
      {stats.map((stat) =>
        stat.lien ? (
          <Link key={stat.libelle} to={stat.lien} className="stat-carte">
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
