import { useParams } from "react-router-dom";

import { mutateBackend } from "@/lib/api";
import { useBackend } from "@/lib/useBackend";
import type { ExamenDto } from "@/types/api";

import { ExamenFormulaire } from "./ExamenFormulaire";

export default function ExamenModifier() {
  const { id } = useParams();
  const { data: examen, chargement } = useBackend<ExamenDto>(`/api/examens/${id}`);

  if (chargement || !examen) {
    return <p className="vide">Chargement…</p>;
  }

  return (
    <ExamenFormulaire
      titre="Modifier l'examen"
      clientNom={examen.clientNomComplet}
      valeurInitiale={examen}
      soumettre={(payload) => mutateBackend(`/api/examens/${id}`, "PUT", payload)}
    />
  );
}
