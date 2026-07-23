import { Link, Outlet } from "react-router-dom";

import { getSession } from "@/lib/session";
import { BoutonDeconnexion } from "./BoutonDeconnexion";

export default function AdminLayout() {
  const session = getSession();

  return (
    <div className="admin-coquille">
      <nav className="admin-nav">
        <div className="marque">AutoEcoleConnect</div>
        <Link to="/admin">Tableau de bord</Link>
        <Link to="/admin/clients">Élèves</Link>
        <Link to="/admin/moniteurs">Moniteurs</Link>
        <Link to="/admin/voitures">Véhicules</Link>
        <Link to="/admin/forfaits">Forfaits</Link>
        <Link to="/admin/reservations">Réservations</Link>
        <Link to="/admin/examens">Examens</Link>
      </nav>
      <div className="admin-contenu">
        <header className="admin-entete">
          <h1>Espace directeur</h1>
          <div className="profil">
            <span>
              {session?.nomComplet} — {session?.role}
            </span>
            <BoutonDeconnexion />
          </div>
        </header>
        <Outlet />
      </div>
    </div>
  );
}
