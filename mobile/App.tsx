import { useEffect, useState } from "react";
import { ActivityIndicator, StyleSheet, View } from "react-native";
import { StatusBar } from "expo-status-bar";
import { NavigationContainer } from "@react-navigation/native";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import { SafeAreaProvider } from "react-native-safe-area-context";

import LoginScreen from "./src/screens/LoginScreen";
import PlanningScreen from "./src/screens/PlanningScreen";
import { getSession } from "./src/lib/session";
import { couleurs } from "./src/theme";
import type { RootStackParamList } from "./src/navigation";

const Stack = createNativeStackNavigator<RootStackParamList>();

export default function App() {
  // null = vérification en cours, sinon la route de démarrage est connue :
  // évite un flash de l'écran de connexion si une session valide existe déjà
  // (persistée dans le Keychain/Keystore d'un lancement précédent).
  const [routeDepart, setRouteDepart] = useState<keyof RootStackParamList | null>(null);

  useEffect(() => {
    getSession().then((session) => {
      setRouteDepart(session?.role === "MONITEUR" ? "Planning" : "Connexion");
    });
  }, []);

  if (!routeDepart) {
    return (
      <View style={styles.chargement}>
        <ActivityIndicator color={couleurs.primaire} />
      </View>
    );
  }

  return (
    <SafeAreaProvider>
      <StatusBar style="light" />
      <NavigationContainer>
        <Stack.Navigator
          initialRouteName={routeDepart}
          screenOptions={{ headerShown: false, contentStyle: { backgroundColor: couleurs.fond } }}
        >
          <Stack.Screen name="Connexion" component={LoginScreen} />
          <Stack.Screen name="Planning" component={PlanningScreen} />
        </Stack.Navigator>
      </NavigationContainer>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  chargement: { flex: 1, backgroundColor: couleurs.fond, alignItems: "center", justifyContent: "center" },
});
