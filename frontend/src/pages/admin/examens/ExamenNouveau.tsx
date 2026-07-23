import { mutateBackend } from "@/lib/api";
import { useBackend } from "@/lib/useBackend";
import type { ClientDto } from "@/types/api";

import { ExamenFormulaire } from "./ExamenFormulaire";

export default function ExamenNouveau() {
  const { data: clients } = useBackend<ClientDto[]>("/api/clients");

  return (
    <ExamenFormulaire
      titre="Nouvel examen"
      clients={clients ?? []}
      soumettre={(payload) => mutateBackend("/api/examens", "POST", payload)}
    />
  );
}
