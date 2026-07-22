import { Outlet } from "react-router-dom";

import { getSession } from "@/lib/session";
import { BoutonDeconnexion } from "@/pages/admin/BoutonDeconnexion";
import { TelechargerMesDonnees } from "./TelechargerMesDonnees";

export default function EleveLayout() {
  const session = getSession();

  return (
    <div>
      <header className="moniteur-entete">
        <span className="marque">AutoEcoleConnect</span>
        <div className="profil">
          <span>{session?.nomComplet}</span>
          <BoutonDeconnexion />
        </div>
      </header>
      <main className="moniteur-contenu">
        <h1>Mon planning</h1>
        <Outlet />
        <TelechargerMesDonnees />
      </main>
    </div>
  );
}
