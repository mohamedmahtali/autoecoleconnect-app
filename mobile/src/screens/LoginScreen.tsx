import { useEffect, useState } from "react";
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import type { NativeStackScreenProps } from "@react-navigation/native-stack";

import { login } from "../lib/api";
import { getEcoleSlug, setSession } from "../lib/session";
import { couleurs, espacements, rayons } from "../theme";
import type { RootStackParamList } from "../navigation";

type Props = NativeStackScreenProps<RootStackParamList, "Connexion">;

export default function LoginScreen({ navigation }: Props) {
  const [ecole, setEcole] = useState("");
  const [email, setEmail] = useState("");
  const [motDePasse, setMotDePasse] = useState("");
  const [erreur, setErreur] = useState<string | null>(null);
  const [chargement, setChargement] = useState(false);

  useEffect(() => {
    getEcoleSlug().then((slug) => {
      if (slug) {
        setEcole(slug);
      }
    });
  }, []);

  async function seConnecter() {
    setErreur(null);
    setChargement(true);
    try {
      const reponse = await login(ecole.trim(), email.trim(), motDePasse);
      if (reponse.role !== "MONITEUR") {
        setErreur("Cette application est réservée aux moniteurs.");
        return;
      }
      await setSession(ecole.trim(), reponse.token);
      navigation.reset({ index: 0, routes: [{ name: "Planning" }] });
    } catch (e) {
      setErreur(e instanceof Error ? e.message : "Connexion impossible");
    } finally {
      setChargement(false);
    }
  }

  return (
    <SafeAreaView style={styles.ecran}>
      <KeyboardAvoidingView
        behavior={Platform.OS === "ios" ? "padding" : undefined}
        style={styles.centre}
      >
        <Text style={styles.titre}>AutoEcoleConnect</Text>
        <Text style={styles.sousTitre}>Espace moniteur</Text>

        {erreur && <Text style={styles.erreur}>{erreur}</Text>}

        <View style={styles.champ}>
          <Text style={styles.libelle}>Identifiant de l’auto-école</Text>
          <TextInput
            style={styles.saisie}
            value={ecole}
            onChangeText={setEcole}
            autoCapitalize="none"
            autoCorrect={false}
            placeholder="ex. auto-ecole-marseille"
            placeholderTextColor={couleurs.texteAttenue}
          />
        </View>

        <View style={styles.champ}>
          <Text style={styles.libelle}>Email</Text>
          <TextInput
            style={styles.saisie}
            value={email}
            onChangeText={setEmail}
            autoCapitalize="none"
            autoCorrect={false}
            keyboardType="email-address"
            placeholderTextColor={couleurs.texteAttenue}
          />
        </View>

        <View style={styles.champ}>
          <Text style={styles.libelle}>Mot de passe</Text>
          <TextInput
            style={styles.saisie}
            value={motDePasse}
            onChangeText={setMotDePasse}
            secureTextEntry
            placeholderTextColor={couleurs.texteAttenue}
          />
        </View>

        <Pressable
          style={({ pressed }) => [styles.bouton, pressed && styles.boutonActif]}
          onPress={seConnecter}
          disabled={chargement || !ecole || !email || !motDePasse}
        >
          {chargement ? (
            <ActivityIndicator color={couleurs.primaireTexte} />
          ) : (
            <Text style={styles.boutonTexte}>Se connecter</Text>
          )}
        </Pressable>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  ecran: { flex: 1, backgroundColor: couleurs.fond },
  centre: { flex: 1, justifyContent: "center", padding: espacements.lg },
  titre: { fontSize: 26, fontWeight: "800", color: couleurs.texte, textAlign: "center" },
  sousTitre: {
    fontSize: 15,
    color: couleurs.texteSecondaire,
    textAlign: "center",
    marginBottom: espacements.lg,
  },
  erreur: { color: couleurs.ko, marginBottom: espacements.md, textAlign: "center" },
  champ: { marginBottom: espacements.md },
  libelle: { color: couleurs.texteSecondaire, fontSize: 13, marginBottom: espacements.xs },
  saisie: {
    borderWidth: 1,
    borderColor: couleurs.bordure,
    borderRadius: rayons.moyen,
    padding: espacements.sm,
    color: couleurs.texte,
    backgroundColor: couleurs.fondCarte,
    fontSize: 16,
  },
  bouton: {
    backgroundColor: couleurs.primaire,
    borderRadius: rayons.moyen,
    paddingVertical: espacements.sm + 2,
    alignItems: "center",
    marginTop: espacements.sm,
  },
  boutonActif: { opacity: 0.85 },
  boutonTexte: { color: couleurs.primaireTexte, fontWeight: "700", fontSize: 16 },
});
