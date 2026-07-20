import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

import { pingBackend } from "@/lib/api";
import type { PingResponse } from "@/types/api";

export default function Home() {
  const [ping, setPing] = useState<PingResponse | null>(null);
  const [verifie, setVerifie] = useState(false);

  useEffect(() => {
    pingBackend().then((reponse) => {
      setPing(reponse);
      setVerifie(true);
    });
  }, []);

  return (
    <div className="plein-ecran-centre">
      <main className="accueil">
        <h1>AutoEcoleConnect</h1>
        <p className="sous-titre">Plateforme SaaS multi-tenant pour auto-écoles</p>
        <section className="statut">
          <h2>État de la stack</h2>
          <ul>
            <li className="ok">✓ frontend — React (Vite) opérationnel</li>
            {!verifie ? (
              <li>… vérification du backend</li>
            ) : ping ? (
              <li className="ok">
                ✓ backend — {ping.service} ({ping.status}, {ping.timestamp})
              </li>
            ) : (
              <li className="ko">✗ backend — injoignable</li>
            )}
          </ul>
        </section>
        <p style={{ marginTop: "2rem" }}>
          <Link to="/login">Se connecter →</Link>
        </p>
      </main>
    </div>
  );
}
