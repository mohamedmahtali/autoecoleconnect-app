import { useCallback, useEffect, useState } from "react";

import { fetchBackend } from "@/lib/api";

// Remplace le data-fetching des anciens Server Components (fetchBackend en
// tête de composant) par un hook client — même contrat d'erreur, plus un
// recharger() pour rafraîchir une liste après une mutation.
export function useBackend<T>(chemin: string) {
  const [data, setData] = useState<T | null>(null);
  const [erreur, setErreur] = useState<string | null>(null);
  const [chargement, setChargement] = useState(true);

  const recharger = useCallback(() => {
    setChargement(true);
    setErreur(null);
    fetchBackend<T>(chemin)
      .then(setData)
      .catch((e: Error) => setErreur(e.message))
      .finally(() => setChargement(false));
  }, [chemin]);

  useEffect(() => {
    recharger();
  }, [recharger]);

  return { data, erreur, chargement, recharger };
}
