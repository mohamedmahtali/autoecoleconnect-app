import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";

import { mutateBackend } from "@/lib/api";

export default function ClientNouveau() {
  const navigate = useNavigate();
  const [erreur, setErreur] = useState<string | null>(null);
  const [enCours, setEnCours] = useState(false);

  async function creer(evenement: FormEvent<HTMLFormElement>) {
    evenement.preventDefault();
    setErreur(null);
    setEnCours(true);
    const donnees = new FormData(evenement.currentTarget);
    const resultat = await mutateBackend("/api/clients", "POST", {
      nom: donnees.get("nom"),
      prenom: donnees.get("prenom"),
      email: donnees.get("email"),
      motDePasse: donnees.get("motDePasse"),
      telephone: donnees.get("telephone") || null,
      adresse: donnees.get("adresse") || null,
      notes: donnees.get("notes") || null,
    });
    setEnCours(false);
    if (!resultat.ok) {
      setErreur(resultat.erreur);
      return;
    }
    navigate("/admin/clients");
  }

  return (
    <section>
      <div className="entete-page">
        <h2>Nouvel élève</h2>
        <Link to="/admin/clients">← Retour à la liste</Link>
      </div>
      <form className="carte" onSubmit={creer} style={{ maxWidth: "28rem" }}>
        {erreur && <p className="erreur">{erreur}</p>}
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
    </section>
  );
}
