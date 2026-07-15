import { pingBackend } from "@/lib/api";

export const dynamic = "force-dynamic";

export default async function Home() {
  const ping = await pingBackend();

  return (
    <main>
      <h1>AutoEcoleConnect</h1>
      <p className="sous-titre">
        Plateforme SaaS multi-tenant pour auto-écoles — Phase 0 : Fondations
      </p>
      <section className="statut">
        <h2>État de la stack locale</h2>
        <ul>
          <li className="ok">✓ frontend — Next.js 15 opérationnel</li>
          {ping ? (
            <li className="ok">
              ✓ backend — {ping.service} ({ping.status}, {ping.timestamp})
            </li>
          ) : (
            <li className="ko">
              ✗ backend — injoignable (démarrer avec docker compose up -d)
            </li>
          )}
        </ul>
      </section>
    </main>
  );
}
