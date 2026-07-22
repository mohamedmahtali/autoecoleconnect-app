import { useState } from "react";

import { fetchBackend } from "@/lib/api";
import type { DonneesPersonnellesDto } from "@/types/api";

// Droit d'accès RGPD self-service : l'élève récupère toutes ses données en un
// fichier JSON. fetchBackend ajoute son jeton ; le backend ne renvoie que les
// données du porteur du jeton (docs/12 §12.6).
export function TelechargerMesDonnees() {
  const [enCours, setEnCours] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);

  async function telecharger() {
    setEnCours(true);
    setErreur(null);
    try {
      const donnees = await fetchBackend<DonneesPersonnellesDto>("/api/eleve/mes-donnees");
      const blob = new Blob([JSON.stringify(donnees, null, 2)], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const lien = document.createElement("a");
      lien.href = url;
      lien.download = "mes-donnees-autoecoleconnect.json";
      lien.click();
      URL.revokeObjectURL(url);
    } catch {
      setErreur("Téléchargement impossible pour le moment. Réessayez.");
    } finally {
      setEnCours(false);
    }
  }

  return (
    <div className="eleve-rgpd">
      <span>Vos données personnelles vous appartiennent (RGPD).</span>
      <button className="btn-ligne" onClick={telecharger} disabled={enCours}>
        {enCours ? "Préparation…" : "Télécharger mes données"}
      </button>
      {erreur && <span className="erreur">{erreur}</span>}
    </div>
  );
}
