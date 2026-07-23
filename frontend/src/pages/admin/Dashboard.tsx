import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

import { fetchBackend } from "@/lib/api";
import type { Identifiable, StatsDto } from "@/types/api";

interface Stat {
  libelle: string;
  valeur: string | number;
  lien?: string;
}

const FORMAT_EUR = new Intl.NumberFormat("fr-FR", { style: "currency", currency: "EUR" });
const FORMAT_POURCENT = new Intl.NumberFormat("fr-FR", { style: "percent", maximumFractionDigits: 0 });

export default function Dashboard() {
  const [stats, setStats] = useState<Stat[] | null>(null);
  const [inscriptions, setInscriptions] = useState<StatsDto["inscriptionsParMois"] | null>(null);

  useEffect(() => {
    Promise.all([
      fetchBackend<Identifiable[]>("/api/clients"),
      fetchBackend<Identifiable[]>("/api/moniteurs"),
      fetchBackend<Identifiable[]>("/api/voitures"),
      fetchBackend<Identifiable[]>("/api/forfaits"),
      fetchBackend<Identifiable[]>("/api/reservations"),
      fetchBackend<Identifiable[]>("/api/seances"),
      fetchBackend<Identifiable[]>("/api/examens"),
      fetchBackend<StatsDto>("/api/stats/resume"),
    ]).then(([clients, moniteurs, voitures, forfaits, reservations, seances, examens, kpi]) => {
      const cartes: Stat[] = [
        { libelle: "Élèves", valeur: clients.length, lien: "/admin/clients" },
        { libelle: "Moniteurs", valeur: moniteurs.length },
        { libelle: "Véhicules", valeur: voitures.length },
        { libelle: "Forfaits", valeur: forfaits.length },
        { libelle: "Réservations", valeur: reservations.length, lien: "/admin/reservations" },
        { libelle: "Séances", valeur: seances.length },
        { libelle: "Examens", valeur: examens.length, lien: "/admin/examens" },
        { libelle: "Chiffre d'affaires encaissé", valeur: FORMAT_EUR.format(kpi.caTotal) },
        { libelle: "Élèves actifs", valeur: kpi.elevesActifs },
        { libelle: "Séances terminées", valeur: kpi.seancesTerminees },
        { libelle: "Taux de no-show", valeur: FORMAT_POURCENT.format(kpi.tauxNoShow) },
      ];
      // Le taux de réussite n'a de sens qu'avec des présentés (sinon 0/0) :
      // on masque la carte tant qu'aucun examen n'a été passé.
      if (kpi.examensPresentes > 0) {
        cartes.push({
          libelle: `Taux de réussite examen (${kpi.examensPresentes} présentés)`,
          valeur: FORMAT_POURCENT.format(kpi.tauxReussiteExamen),
        });
      }
      setStats(cartes);
      setInscriptions(kpi.inscriptionsParMois);
    });
  }, []);

  if (!stats) {
    return <p className="vide">Chargement…</p>;
  }

  return (
    <>
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

      {inscriptions && inscriptions.length > 0 && (
        <section className="inscriptions-mensuelles">
          <h2>Inscriptions par mois</h2>
          <ul>
            {inscriptions.map((ligne) => (
              <li key={ligne.mois}>
                <span>{ligne.mois}</span>
                <span>{ligne.nombre}</span>
              </li>
            ))}
          </ul>
        </section>
      )}
    </>
  );
}
