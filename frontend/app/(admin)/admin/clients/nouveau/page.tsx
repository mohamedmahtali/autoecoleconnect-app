import Link from "next/link";

import { FormulaireNouvelEleve } from "./formulaire";

export default function PageNouvelEleve() {
  return (
    <section>
      <div className="entete-page">
        <h2>Nouvel élève</h2>
        <Link href="/admin/clients">← Retour à la liste</Link>
      </div>
      <FormulaireNouvelEleve />
    </section>
  );
}
