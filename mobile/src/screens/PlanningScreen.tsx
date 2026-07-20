import { useCallback, useEffect, useState } from "react";
import { FlatList, RefreshControl, StyleSheet, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";

import { fetchBackend, mutateBackend } from "../lib/api";
import { clearToken } from "../lib/session";
import { SeanceCard } from "../components/SeanceCard";
import { couleurs, espacements } from "../theme";
import type { RootStackParamList } from "../navigation";
import type { SeanceDto } from "../types/api";

type Props = NativeStackScreenProps<RootStackParamList, "Planning">;

export default function PlanningScreen({ navigation }: Props) {
  const [seances, setSeances] = useState<SeanceDto[] | null>(null);
  const [rafraichit, setRafraichit] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);

  const charger = useCallback(async () => {
    try {
      const donnees = await fetchBackend<SeanceDto[]>("/api/seances");
      setSeances(donnees);
      setErreur(null);
    } catch (e) {
      setErreur(e instanceof Error ? e.message : "Impossible de charger le planning");
    }
  }, []);

  useEffect(() => {
    charger();
  }, [charger]);

  async function rafraichir() {
    setRafraichit(true);
    await charger();
    setRafraichit(false);
  }

  async function confirmer(id: string) {
    await mutateBackend(`/api/seances/${id}/validation-moniteur`, "PATCH");
    charger();
  }

  async function seDeconnecter() {
    await clearToken();
    navigation.reset({ index: 0, routes: [{ name: "Connexion" }] });
  }

  const triees = seances
    ? [...seances].sort((a, b) => `${a.dateSeance}${a.hDeb}`.localeCompare(`${b.dateSeance}${b.hDeb}`))
    : [];

  return (
    <SafeAreaView style={styles.ecran} edges={["top", "bottom"]}>
      <View style={styles.entete}>
        <Text style={styles.titre}>Mon planning</Text>
        <Text style={styles.deconnexion} onPress={seDeconnecter}>
          Se déconnecter
        </Text>
      </View>

      {erreur && <Text style={styles.erreur}>{erreur}</Text>}

      <FlatList
        data={triees}
        keyExtractor={(seance) => seance.id}
        contentContainerStyle={styles.liste}
        renderItem={({ item }) => <SeanceCard seance={item} surConfirmer={confirmer} />}
        refreshControl={
          <RefreshControl refreshing={rafraichit} onRefresh={rafraichir} tintColor={couleurs.primaire} />
        }
        ListEmptyComponent={
          seances ? <Text style={styles.vide}>Aucune séance planifiée pour l’instant.</Text> : null
        }
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  ecran: { flex: 1, backgroundColor: couleurs.fond },
  entete: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    paddingHorizontal: espacements.lg,
    paddingVertical: espacements.md,
    borderBottomWidth: 1,
    borderBottomColor: couleurs.bordure,
  },
  titre: { color: couleurs.texte, fontSize: 20, fontWeight: "700" },
  deconnexion: { color: couleurs.texteAttenue, fontSize: 13 },
  erreur: { color: couleurs.ko, padding: espacements.md, textAlign: "center" },
  liste: { padding: espacements.lg, flexGrow: 1 },
  vide: { color: couleurs.texteAttenue, textAlign: "center", marginTop: espacements.lg },
});
