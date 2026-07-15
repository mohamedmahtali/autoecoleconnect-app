"use client";

import { useState, type FormEvent, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";

function FormulaireLogin() {
  const router = useRouter();
  const parametres = useSearchParams();
  const [email, setEmail] = useState("");
  const [motDePasse, setMotDePasse] = useState("");
  const [erreur, setErreur] = useState<string | null>(null);
  const [chargement, setChargement] = useState(false);

  async function seConnecter(evenement: FormEvent) {
    evenement.preventDefault();
    setErreur(null);
    setChargement(true);
    try {
      const res = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, motDePasse }),
      });
      const corps = await res.json();
      if (!res.ok) {
        setErreur(corps.detail ?? "Connexion impossible");
        return;
      }
      const destinations: Record<string, string> = {
        DIRECTEUR: "/admin",
        MONITEUR: "/",
        CLIENT: "/",
      };
      router.push(parametres.get("redirige") ?? destinations[corps.role] ?? "/");
      router.refresh();
    } catch {
      setErreur("Le serveur ne répond pas");
    } finally {
      setChargement(false);
    }
  }

  return (
    <form className="carte" onSubmit={seConnecter}>
      <h1>Connexion</h1>
      <p className="sous-titre">AutoEcoleConnect — espace de gestion</p>
      {erreur && <p className="erreur">{erreur}</p>}
      <label className="champ">
        <span>Email</span>
        <input
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          autoComplete="email"
          required
        />
      </label>
      <label className="champ">
        <span>Mot de passe</span>
        <input
          type="password"
          value={motDePasse}
          onChange={(e) => setMotDePasse(e.target.value)}
          autoComplete="current-password"
          required
        />
      </label>
      <button className="bouton" type="submit" disabled={chargement}>
        {chargement ? "Connexion…" : "Se connecter"}
      </button>
    </form>
  );
}

export default function PageLogin() {
  return (
    <div className="plein-ecran-centre">
      <Suspense>
        <FormulaireLogin />
      </Suspense>
    </div>
  );
}
