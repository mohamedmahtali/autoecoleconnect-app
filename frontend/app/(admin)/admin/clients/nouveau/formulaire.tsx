"use client";

import { useActionState } from "react";

import { creerClient, type EtatFormulaire } from "../actions";

const etatInitial: EtatFormulaire = { erreur: null };

export function FormulaireNouvelEleve() {
  const [etat, action, enCours] = useActionState(creerClient, etatInitial);

  return (
    <form className="carte" action={action} style={{ maxWidth: "28rem" }}>
      {etat.erreur && <p className="erreur">{etat.erreur}</p>}
      <label className="champ">
        <span>Prénom</span>
        <input name="prenom" required maxLength={100} />
      </label>
      <label className="champ">
        <span>Nom</span>
        <input name="nom" required maxLength={100} />
      </label>
      <label className="champ">
        <span>Email</span>
        <input name="email" type="email" required maxLength={255} />
      </label>
      <label className="champ">
        <span>Mot de passe initial (8 caractères min.)</span>
        <input name="motDePasse" type="password" required minLength={8} maxLength={72} />
      </label>
      <label className="champ">
        <span>Téléphone (optionnel)</span>
        <input name="telephone" maxLength={20} />
      </label>
      <label className="champ">
        <span>Adresse (optionnel)</span>
        <input name="adresse" />
      </label>
      <label className="champ">
        <span>Notes (optionnel)</span>
        <input name="notes" />
      </label>
      <button className="bouton" type="submit" disabled={enCours}>
        {enCours ? "Création…" : "Créer l’élève"}
      </button>
    </form>
  );
}
