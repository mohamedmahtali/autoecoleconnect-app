"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";

import { mutateBackend } from "@/lib/api";

export interface EtatFormulaire {
  erreur: string | null;
}

// Server Action : s'exécute côté serveur, le token reste dans le cookie
// httpOnly — le navigateur ne voit ni le JWT ni l'URL du backend.
export async function creerClient(
  _etatPrecedent: EtatFormulaire,
  formData: FormData,
): Promise<EtatFormulaire> {
  const resultat = await mutateBackend("/api/clients", "POST", {
    nom: formData.get("nom"),
    prenom: formData.get("prenom"),
    email: formData.get("email"),
    motDePasse: formData.get("motDePasse"),
    telephone: formData.get("telephone") || null,
    adresse: formData.get("adresse") || null,
    notes: formData.get("notes") || null,
  });
  if (!resultat.ok) {
    return { erreur: resultat.erreur };
  }
  revalidatePath("/admin/clients");
  redirect("/admin/clients");
}
