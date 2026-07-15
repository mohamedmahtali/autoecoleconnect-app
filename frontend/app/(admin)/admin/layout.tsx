import Link from "next/link";

import { getSession } from "@/lib/session";
import { BoutonDeconnexion } from "./bouton-deconnexion";

export default async function AdminLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  const session = await getSession();

  return (
    <div className="admin-coquille">
      <nav className="admin-nav">
        <div className="marque">AutoEcoleConnect</div>
        <Link href="/admin">Tableau de bord</Link>
        <Link href="/admin/clients">Élèves</Link>
        <Link href="/admin/moniteurs">Moniteurs</Link>
        <Link href="/admin/voitures">Véhicules</Link>
        <Link href="/admin/forfaits">Forfaits</Link>
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
        {children}
      </div>
    </div>
  );
}
