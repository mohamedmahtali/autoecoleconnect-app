import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";

import type { ResultatMutation } from "@/lib/api";
import type { ClientDto, ExamenDto, ResultatExamen, TypeExamen } from "@/types/api";

const TYPES: { valeur: TypeExamen; libelle: string }[] = [
  { valeur: "CODE", libelle: "Code" },
  { valeur: "CONDUITE", libelle: "Conduite" },
];

const RESULTATS: { valeur: ResultatExamen; libelle: string }[] = [
  { valeur: "PLANIFIE", libelle: "Planifié (convoqué, pas encore passé)" },
  { valeur: "REUSSI", libelle: "Réussi" },
  { valeur: "ECHOUE", libelle: "Échoué" },
  { valeur: "ABSENT", libelle: "Absent" },
];

interface Props {
  titre: string;
  // Création : liste d'élèves à choisir. Édition : nom figé (un examen ne se
  // réassigne pas à un autre élève).
  clients?: ClientDto[];
  clientNom?: string;
  valeurInitiale?: Partial<ExamenDto>;
  soumettre: (payload: Record<string, unknown>) => Promise<ResultatMutation>;
}

export function ExamenFormulaire({ titre, clients, clientNom, valeurInitiale, soumettre }: Props) {
  const navigate = useNavigate();
  const [erreur, setErreur] = useState<string | null>(null);
  const [enCours, setEnCours] = useState(false);
  const v = valeurInitiale ?? {};

  async function onSubmit(evenement: FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    setErreur(null);
    setEnCours(true);
    const d = new FormData(evenement.currentTarget);
    const fautes = d.get("nombreFautes") as string;
    const payload: Record<string, unknown> = {
      type: d.get("type"),
      dateExamen: d.get("dateExamen"),
      dateConvocation: d.get("dateConvocation") || null,
      resultat: d.get("resultat"),
      nombreFautes: fautes ? Number(fautes) : null,
      centreExamen: d.get("centreExamen") || null,
      examinateur: d.get("examinateur") || null,
      notes: d.get("notes") || null,
    };
    if (clients) {
      payload.clientId = d.get("clientId");
    }
    const resultat = await soumettre(payload);
    setEnCours(false);
    if (!resultat.ok) {
      setErreur(resultat.erreur);
      return;
    }
    navigate("/admin/examens");
  }

  return (
    <section>
      <div className="entete-page">
        <h2>{titre}</h2>
        <Link to="/admin/examens">← Retour à la liste</Link>
      </div>
      <form className="carte" onSubmit={onSubmit} style={{ maxWidth: "28rem" }}>
        {erreur && <p className="erreur">{erreur}</p>}
        <label className="champ">
          <span>Élève</span>
          {clients ? (
            <select name="clientId" required defaultValue="">
              <option value="" disabled>
                Choisir un élève…
              </option>
              {clients.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.prenom} {c.nom}
                </option>
              ))}
            </select>
          ) : (
            <input value={clientNom ?? ""} disabled />
          )}
        </label>
        <label className="champ">
          <span>Type d'examen</span>
          <select name="type" defaultValue={v.type ?? "CONDUITE"}>
            {TYPES.map((t) => (
              <option key={t.valeur} value={t.valeur}>
                {t.libelle}
              </option>
            ))}
          </select>
        </label>
        <label className="champ">
          <span>Date de l'examen</span>
          <input name="dateExamen" type="date" required defaultValue={v.dateExamen ?? ""} />
        </label>
        <label className="champ">
          <span>Date de convocation (optionnel)</span>
          <input name="dateConvocation" type="date" defaultValue={v.dateConvocation ?? ""} />
        </label>
        <label className="champ">
          <span>Résultat</span>
          <select name="resultat" defaultValue={v.resultat ?? "PLANIFIE"}>
            {RESULTATS.map((r) => (
              <option key={r.valeur} value={r.valeur}>
                {r.libelle}
              </option>
            ))}
          </select>
        </label>
        <label className="champ">
          <span>Nombre de fautes (optionnel)</span>
          <input name="nombreFautes" type="number" min="0" defaultValue={v.nombreFautes ?? ""} />
        </label>
        <label className="champ">
          <span>Centre d'examen (optionnel)</span>
          <input name="centreExamen" defaultValue={v.centreExamen ?? ""} />
        </label>
        <label className="champ">
          <span>Examinateur (optionnel)</span>
          <input name="examinateur" defaultValue={v.examinateur ?? ""} />
        </label>
        <label className="champ">
          <span>Notes (optionnel)</span>
          <input name="notes" defaultValue={v.notes ?? ""} />
        </label>
        <button className="bouton" type="submit" disabled={enCours}>
          {enCours ? "Enregistrement…" : "Enregistrer"}
        </button>
      </form>
    </section>
  );
}
