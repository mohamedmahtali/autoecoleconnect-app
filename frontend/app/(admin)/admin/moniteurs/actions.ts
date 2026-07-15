"use server";

import { revalidatePath } from "next/cache";

import { mutateBackend } from "@/lib/api";
import type { StatutMoniteur } from "@/types/api";

export async function changerStatutMoniteur(id: string, statut: StatutMoniteur) {
  // Les transitions interdites sont refusées par le backend (400) ; l'UI ne
  // proposant que les boutons valides, on se contente de rafraîchir.
  await mutateBackend(`/api/moniteurs/${id}/statut`, "PATCH", { statut });
  revalidatePath("/admin/moniteurs");
}
