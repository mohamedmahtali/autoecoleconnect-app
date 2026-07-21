import { StyleSheet, Text, View } from "react-native";

import { badges, couleurs, espacements, rayons } from "../theme";
import type { SeanceDto } from "../types/api";

const LIBELLES_STATUT: Record<SeanceDto["statut"], string> = {
  SCHEDULED: "Planifiée",
  COMPLETED: "Terminée",
  CANCELLED: "Annulée",
  NO_SHOW: "Absence",
};

// Même mapping sémantique que la SPA (frontend/src/pages/moniteur/Planning.tsx) :
// une seule palette de badges réutilisée pour tous les statuts de l'app.
const BADGE: Record<SeanceDto["statut"], { fond: string; texte: string }> = {
  SCHEDULED: badges.attente,
  COMPLETED: badges.approuve,
  CANCELLED: badges.inactif,
  NO_SHOW: badges.rejete,
};

function formaterHeure(heure: string) {
  return heure.slice(0, 5);
}

function formaterDate(date: string) {
  const texte = new Date(`${date}T00:00:00`).toLocaleDateString("fr-FR", {
    weekday: "long",
    day: "numeric",
    month: "long",
  });
  return texte.charAt(0).toUpperCase() + texte.slice(1);
}

export function SeanceCard({
  seance,
  role,
  surConfirmer,
}: {
  seance: SeanceDto;
  role: "MONITEUR" | "CLIENT";
  surConfirmer: (id: string) => void;
}) {
  const badge = BADGE[seance.statut];
  const autrePartie = role === "CLIENT" ? seance.moniteurNomComplet : seance.clientNomComplet;
  const validee = role === "CLIENT" ? seance.validatedClient : seance.validatedMoniteur;

  return (
    <View style={styles.carte}>
      <View style={styles.entete}>
        <Text style={styles.date}>{formaterDate(seance.dateSeance)}</Text>
        <View style={[styles.badge, { backgroundColor: badge.fond }]}>
          <Text style={[styles.badgeTexte, { color: badge.texte }]}>
            {LIBELLES_STATUT[seance.statut]}
          </Text>
        </View>
      </View>

      <Text style={styles.horaire}>
        {formaterHeure(seance.hDeb)} – {formaterHeure(seance.hFin)}
      </Text>
      {autrePartie && <Text style={styles.detail}>{autrePartie}</Text>}
      {seance.voitureNom && <Text style={styles.detail}>{seance.voitureNom}</Text>}

      {seance.statut === "SCHEDULED" && (
        <View style={styles.actions}>
          {validee ? (
            <Text style={styles.confirme}>✓ Présence confirmée</Text>
          ) : (
            <Text style={styles.lienConfirmer} onPress={() => surConfirmer(seance.id)}>
              Confirmer ma présence
            </Text>
          )}
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  carte: {
    backgroundColor: couleurs.fondCarte,
    borderWidth: 1,
    borderColor: couleurs.bordure,
    borderRadius: rayons.moyen,
    padding: espacements.md,
    marginBottom: espacements.sm,
  },
  entete: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: espacements.xs,
  },
  date: { color: couleurs.texte, fontWeight: "600", fontSize: 15 },
  badge: { borderRadius: rayons.pilule, paddingVertical: 3, paddingHorizontal: 10 },
  badgeTexte: { fontSize: 12, fontWeight: "700" },
  horaire: {
    color: couleurs.texte,
    fontVariant: ["tabular-nums"],
    fontSize: 15,
    marginBottom: 4,
  },
  detail: { color: couleurs.texteSecondaire, fontSize: 14 },
  actions: {
    marginTop: espacements.sm,
    paddingTop: espacements.sm,
    borderTopWidth: 1,
    borderTopColor: couleurs.bordure,
  },
  confirme: { color: couleurs.ok, fontWeight: "600" },
  lienConfirmer: { color: couleurs.primaire, fontWeight: "700" },
});
