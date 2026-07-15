---
noteId: "38daec617f8711f1878859078c773cc2"
tags: []

---

# 13. Analytics — Tableau de bord statistiques du directeur

## 13.1 Onglet Statistiques — vue d'ensemble

Le directeur dispose d'un onglet dédié dans son espace d'administration. L'objectif : comprendre son activité au-delà du chiffre d'affaires — pour décider quand embaucher un moniteur supplémentaire, quand lancer une promo, ou évaluer la qualité pédagogique de l'auto-école.

```
┌────────────────────────────────────────────────────────────────────────┐
│  Auto-École Lyon Centre — Statistiques                                 │
├──────────┬───────────────┬─────────────────────────────────────────────┤
│  Tableau │  Réservations │  Statistiques ← onglet actif               │
│  de bord │  Paiements    │                                             │
└──────────┴───────────────┴─────────────────────────────────────────────┘

  Période :  ○ 12 derniers mois  ○ Année civile  ○ Personnalisée

┌─────────────────────────────────────────────────────────────────────────┐
│  Inscriptions par mois                                                  │
│                                                                         │
│  25 ┤                         ██                                        │
│  20 ┤              ██    ██   ██  ██                                    │
│  15 ┤         ██   ██    ██   ██  ██   ██                              │
│  10 ┤    ██   ██   ██    ██   ██  ██   ██   ██   ██                   │
│   5 ┤ ██ ██   ██   ██    ██   ██  ██   ██   ██   ██   ██   ██        │
│   0 ┼────────────────────────────────────────────────────────          │
│      Jan Fév Mar  Avr  Mai Jui Jui Aoû Sep Oct  Nov  Déc              │
│                                                                         │
│  ↑ Pic de juin-juillet : rentrée auto-école après le bac               │
│  ↑ Pic de septembre : rentrée universitaire                            │
│                                                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Temps moyen pour obtenir le permis                                     │
│                                                                         │
│  Tous les élèves (reçus) :         4 mois 12 jours                     │
│  Plan Accéléré (20h) :             2 mois 8 jours                      │
│  Plan Standard (30h) :             5 mois 3 jours                      │
│                                                                         │
│  Meilleur élève :  38 jours                                            │
│  Le plus long    :  14 mois 6 jours                                    │
│                                                                         │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  Distribution (élèves reçus)                                     │  │
│  │  < 2 mois   ██░░░░░░░░░░░░░░  8%                                │  │
│  │  2-3 mois   ████████████░░░░  31%                               │  │
│  │  3-5 mois   ████████████████  42%                               │  │
│  │  5-8 mois   ████████░░░░░░░░  14%                               │  │
│  │  > 8 mois   ███░░░░░░░░░░░░░   5%                               │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Taux de réussite à l'examen                                           │
│                                                                         │
│  Code :       85%   ████████████████████░░░░                           │
│  Conduite :   71%   █████████████████░░░░░░░                           │
│  Moyenne nationale (conduite) : 57%  →  +14 pts                        │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 13.2 Table `resultats_examen` — ajout au schéma tenant

Ce KPI nécessite d'enregistrer les résultats d'examen. On ajoute cette table au schéma BDD tenant (migration Liquibase) :

```sql
-- Migration Liquibase : db/changelog/v1.2/001-add-resultats-examen.sql
-- (convention du projet — cf. section 6 : dossier par version, fichiers numérotés)
CREATE TABLE resultats_examen (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id       UUID NOT NULL REFERENCES clients(id),
    type_examen     VARCHAR(20) NOT NULL,       -- 'code' | 'conduite'
    date_examen     DATE NOT NULL,
    resultat        VARCHAR(10) NOT NULL,        -- 'reussi' | 'echoue'
    centre_examen   VARCHAR(255),
    nb_tentative    INTEGER NOT NULL DEFAULT 1,  -- numéro de la tentative
    notes           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_resultats_client   ON resultats_examen(client_id);
CREATE INDEX idx_resultats_date     ON resultats_examen(date_examen);
CREATE INDEX idx_resultats_resultat ON resultats_examen(resultat);
```

Le directeur renseigne le résultat depuis l'interface après chaque examen. Un élève peut avoir plusieurs lignes (plusieurs tentatives).

---

## 13.3 Requêtes SQL — KPI saisonnalité

**Inscriptions par mois sur les 12 derniers mois :**

```sql
SELECT
    TO_CHAR(created_at, 'YYYY-MM')        AS mois,
    TO_CHAR(created_at, 'Mon YYYY')       AS mois_label,
    COUNT(*)                               AS nb_inscriptions
FROM clients
WHERE created_at >= NOW() - INTERVAL '12 months'
GROUP BY 1, 2
ORDER BY 1;
```

**Moyenne par mois calendaire (toute l'historique) — pour détecter la saisonnalité :**

```sql
-- "En moyenne, combien de nouveaux élèves chaque mois de l'année ?"
SELECT
    EXTRACT(MONTH FROM created_at)         AS numero_mois,
    TO_CHAR(created_at, 'Month')           AS nom_mois,
    ROUND(COUNT(*) / COUNT(DISTINCT EXTRACT(YEAR FROM created_at)), 1) AS moyenne_eleves
FROM clients
GROUP BY 1, 2
ORDER BY 1;
```

Résultat type :
```
numero_mois | nom_mois  | moyenne_eleves
------------+-----------+---------------
1           | January   | 4.0
2           | February  | 3.5
6           | June      | 18.0    ← pic post-bac
7           | July      | 22.5    ← pic estival
9           | September | 14.0    ← rentrée universitaire
```

---

## 13.4 Requêtes SQL — KPI temps pour obtenir le permis

**Temps moyen entre inscription et réussite à la conduite :**

```sql
SELECT
    f.nom                                   AS forfait,
    COUNT(*)                                AS nb_eleves_recus,
    ROUND(AVG(
        EXTRACT(DAY FROM re.date_examen::timestamp - c.created_at)
    ))                                      AS jours_moyens,
    ROUND(AVG(
        EXTRACT(DAY FROM re.date_examen::timestamp - c.created_at)
    ) / 30.0, 1)                           AS mois_moyens,
    MIN(EXTRACT(DAY FROM re.date_examen::timestamp - c.created_at))
                                            AS jours_min,
    MAX(EXTRACT(DAY FROM re.date_examen::timestamp - c.created_at))
                                            AS jours_max
FROM resultats_examen re
JOIN clients c ON c.id = re.client_id
-- le forfait est porté par la réservation, pas par le client :
-- on prend le premier forfait souscrit par l'élève (LATERAL join)
JOIN LATERAL (
    SELECT fo.nom
    FROM reservations r
    JOIN forfaits fo ON fo.id = r.forfait_id
    WHERE r.client_id = c.id
    ORDER BY r.created_at ASC
    LIMIT 1
) f ON true
WHERE re.type_examen = 'conduite'
  AND re.resultat    = 'reussi'
  AND re.nb_tentative = (
      -- garder uniquement la réussite (pas les échecs précédents)
      SELECT MIN(r2.nb_tentative)
      FROM resultats_examen r2
      WHERE r2.client_id   = re.client_id
        AND r2.type_examen = 'conduite'
        AND r2.resultat    = 'reussi'
  )
GROUP BY f.nom
ORDER BY jours_moyens;
```

**Distribution en tranches (pour l'histogramme) :**

```sql
SELECT
    CASE
        WHEN jours <  60  THEN '< 2 mois'
        WHEN jours <  90  THEN '2-3 mois'
        WHEN jours < 150  THEN '3-5 mois'
        WHEN jours < 240  THEN '5-8 mois'
        ELSE                   '> 8 mois'
    END                              AS tranche,
    COUNT(*)                         AS nb_eleves,
    ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER (), 1) AS pourcentage
FROM (
    SELECT
        EXTRACT(DAY FROM re.date_examen::timestamp - c.created_at) AS jours
    FROM resultats_examen re
    JOIN clients c ON c.id = re.client_id
    WHERE re.type_examen = 'conduite'
      AND re.resultat    = 'reussi'
) sub
GROUP BY 1
ORDER BY MIN(jours);
```

---

## 13.5 Endpoint Spring Boot — `/api/v1/stats`

```java
@RestController
@RequestMapping("/api/v1/stats")
public class StatsController {

    private final StatsService statsService;

    // Saisonnalité — inscriptions par mois
    @GetMapping("/inscriptions-par-mois")
    public List<MoisStat> inscriptionsParMois(
        @RequestParam(defaultValue = "12") int derniersMois
    ) {
        return statsService.inscriptionsParMois(derniersMois);
    }

    // Temps moyen pour obtenir le permis
    @GetMapping("/temps-permis")
    public TempsPermisStat tempsPermis() {
        return statsService.tempsPermis();
    }

    // Taux de réussite aux examens
    @GetMapping("/taux-reussite")
    public TauxReussiteStat tauxReussite(
        @RequestParam(defaultValue = "12") int derniersMois
    ) {
        return statsService.tauxReussite(derniersMois);
    }
}

// DTOs de réponse
public record MoisStat(
    String mois,           // "2026-06"
    String moisLabel,      // "Juin 2026"
    int    nbInscriptions
) {}

public record TempsPermisStat(
    double   moyenneJours,
    double   moyenneMois,
    int      joursMin,
    int      joursMax,
    int      nbElevesRecus,
    List<TrancheStat> distribution,
    List<ForfaitStat> parForfait
) {}

public record TrancheStat(
    String tranche,        // "3-5 mois"
    int    nbEleves,
    double pourcentage
) {}
```

---

## 13.6 Frontend Next.js — composant graphique

Pour les graphiques, on utilise **Recharts** (léger, natif React, pas de dépendance D3) :

```bash
npm install recharts
```

```tsx
// components/stats/InscriptionsChart.tsx
"use client";

import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer
} from "recharts";

type MoisStat = {
  moisLabel: string;
  nbInscriptions: number;
};

export function InscriptionsChart({ data }: { data: MoisStat[] }) {
  return (
    <div className="rounded-xl border p-6">
      <h2 className="text-lg font-semibold mb-4">Inscriptions par mois</h2>
      <ResponsiveContainer width="100%" height={240}>
        <BarChart data={data} margin={{ top: 4, right: 8, bottom: 4, left: 0 }}>
          <CartesianGrid strokeDasharray="3 3" vertical={false} />
          <XAxis
            dataKey="moisLabel"
            tick={{ fontSize: 12 }}
            tickFormatter={(v) => v.slice(0, 3)}   // "Jan", "Fév"...
          />
          <YAxis tick={{ fontSize: 12 }} allowDecimals={false} />
          <Tooltip
            formatter={(value) => [`${value} inscriptions`]}
          />
          <Bar dataKey="nbInscriptions" fill="#3b82f6" radius={[4, 4, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
```

```tsx
// app/dashboard/stats/page.tsx
import { InscriptionsChart } from "@/components/stats/InscriptionsChart";

// Taux national de réussite à l'épreuve pratique — publié chaque année
// par la Sécurité routière. À terme : variable d'env ou config tenant.
const MOYENNE_NATIONALE_CONDUITE = 57;

export default async function StatsPage() {
  // Fetch côté serveur (Next.js Server Component)
  const [inscriptions, tempsPermis, tauxReussite] = await Promise.all([
    fetch(`${process.env.BACKEND_URL}/api/v1/stats/inscriptions-par-mois`).then(r => r.json()),
    fetch(`${process.env.BACKEND_URL}/api/v1/stats/temps-permis`).then(r => r.json()),
    fetch(`${process.env.BACKEND_URL}/api/v1/stats/taux-reussite`).then(r => r.json()),
  ]);

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Statistiques</h1>

      {/* Saisonnalité */}
      <InscriptionsChart data={inscriptions} />

      {/* Temps moyen */}
      <div className="rounded-xl border p-6">
        <h2 className="text-lg font-semibold mb-4">
          Temps moyen pour obtenir le permis
        </h2>
        <p className="text-4xl font-bold text-blue-600">
          {tempsPermis.moyenneMois} mois
        </p>
        <p className="text-sm text-gray-500 mt-1">
          Sur {tempsPermis.nbElevesRecus} élèves reçus
          · min {tempsPermis.joursMin}j · max {tempsPermis.joursMax}j
        </p>
        {/* Distribution en barres */}
        {tempsPermis.distribution.map((t: TrancheStat) => (
          <div key={t.tranche} className="mt-3">
            <div className="flex justify-between text-sm mb-1">
              <span>{t.tranche}</span>
              <span>{t.pourcentage}%</span>
            </div>
            <div className="h-2 rounded-full bg-gray-100">
              <div
                className="h-2 rounded-full bg-blue-500"
                style={{ width: `${t.pourcentage}%` }}
              />
            </div>
          </div>
        ))}
      </div>

      {/* Taux de réussite */}
      <div className="rounded-xl border p-6 grid grid-cols-2 gap-6">
        <div>
          <p className="text-sm text-gray-500">Code de la route</p>
          <p className="text-3xl font-bold">{tauxReussite.code}%</p>
        </div>
        <div>
          <p className="text-sm text-gray-500">Conduite</p>
          <p className="text-3xl font-bold">{tauxReussite.conduite}%</p>
          <p className="text-xs text-green-600 mt-1">
            +{(tauxReussite.conduite - MOYENNE_NATIONALE_CONDUITE).toFixed(0)} pts vs moyenne nationale
          </p>
        </div>
      </div>
    </div>
  );
}
```

---

## 13.7 Checklist d'implémentation

```
□ Migration Liquibase v1.2/001-add-resultats-examen.sql
□ Interface directeur — saisie résultat examen (formulaire simple)
□ 5 endpoints Spring Boot (/stats/inscriptions-par-mois, /temps-permis, /taux-reussite, /taux-occupation, /taux-abandon)
□ Tests unitaires sur StatsService (données de fixtures)
□ Composant InscriptionsChart (Recharts)
□ Page /dashboard/stats avec les 5 blocs
□ Vérifier que les requêtes SQL tiennent bien dans les limites du plan Solo
  (index sur created_at et date_examen déjà prévus)
```

---

## 13.8 KPI — Taux d'occupation des créneaux

C'est le KPI de rentabilité n°1 : un créneau de moniteur non réservé est une heure de salaire perdue.

```
Formule :
  taux_occupation = nb_créneaux_réservés / nb_créneaux_disponibles × 100

Seuils d'interprétation :
  < 60%  → sous-utilisation — action marketing nécessaire
  60-80% → zone saine
  80-90% → bonne performance
  > 90%  → saturation — envisager un moniteur supplémentaire
```

**Schéma impliqué :** la table `moniteur_disponibilites` contient les créneaux proposés par chaque moniteur. La table `reservations` contient les créneaux effectivement réservés.

```sql
-- Le volume disponible vient des créneaux hebdo des moniteurs
-- (moniteur_disponibilites), le volume réservé des séances planifiées (seances).
-- On compare des HEURES, pas des lignes — les deux tables n'ont pas la même granularité.

WITH heures_disponibles AS (
    -- créneaux hebdo convertis en heures sur 30 jours (~4.3 semaines/mois)
    SELECT
        moniteur_id,
        SUM(EXTRACT(EPOCH FROM (heure_fin - heure_debut)) / 3600) * 4.3 AS heures
    FROM moniteur_disponibilites
    WHERE type_disponibilite = 'weekly_slot'
      AND active = true
    GROUP BY moniteur_id
),
heures_reservees AS (
    SELECT
        moniteur_id,
        SUM(EXTRACT(EPOCH FROM (h_fin - h_deb)) / 3600) AS heures
    FROM seances
    WHERE date_seance >= NOW() - INTERVAL '30 days'
      AND date_seance <  NOW()
      AND statut NOT IN ('cancelled', 'no_show')
    GROUP BY moniteur_id
)
-- Par moniteur + ligne TOTAL (GROUP BY ROLLUP)
SELECT
    COALESCE(m.prenom || ' ' || m.nom, '— TOTAL —')     AS moniteur,
    ROUND(SUM(COALESCE(hr.heures, 0)))                   AS heures_reservees,
    ROUND(SUM(hd.heures))                                AS heures_disponibles,
    ROUND(SUM(COALESCE(hr.heures, 0)) * 100.0
          / NULLIF(SUM(hd.heures), 0), 1)                AS taux_pct
FROM moniteurs m
JOIN heures_disponibles hd ON hd.moniteur_id = m.id
LEFT JOIN heures_reservees hr ON hr.moniteur_id = m.id
GROUP BY ROLLUP ((m.prenom || ' ' || m.nom))
ORDER BY taux_pct DESC;
```

**Endpoint :**

```java
@GetMapping("/taux-occupation")
public TauxOccupationStat tauxOccupation(
    @RequestParam(defaultValue = "30") int dernierJours
) {
    return statsService.tauxOccupation(dernierJours);
}

public record TauxOccupationStat(
    int    creneauxDisponibles,
    int    creneauxReserves,
    double tauxGlobal,
    String interpretation,          // "Zone saine", "Saturation", ...
    List<MoniteurOccupation> parMoniteur
) {}

public record MoniteurOccupation(
    String moniteur,
    int    creneauxDisponibles,
    int    creneauxReserves,
    double taux
) {}
```

**Affichage dans le dashboard :**

```
┌──────────────────────────────────────────────────────────────────┐
│  Taux d'occupation — 30 derniers jours                          │
│                                                                  │
│            78%                                                   │
│     ████████████████░░░░░  Zone saine ✓                         │
│                                                                  │
│  Par moniteur :                                                  │
│  Jean Dupont     ████████████████████░  91%  ⚠ Saturé          │
│  Marie Martin    ████████████░░░░░░░░░  62%  ✓                  │
│  Paul Bernard    ████████░░░░░░░░░░░░░  48%  ↓ Sous-utilisé    │
└──────────────────────────────────────────────────────────────────┘
```

---

## 13.9 KPI — Taux d'abandon

Le taux d'abandon mesure la proportion d'élèves qui s'inscrivent mais ne passent jamais leur examen. C'est souvent le signal le plus ignoré — et pourtant il révèle des problèmes structurels (prix trop élevés, mauvaises disponibilités, pédagogie).

```
Formule :
  taux_abandon = élèves sans activité depuis > 3 mois / total élèves inscrits × 100

Définition d'un abandon :
  - Inscrit depuis > 90 jours
  - Pas de réservation dans les 60 derniers jours
  - Pas de résultat d'examen (conduite réussie)
```

```sql
-- Élèves considérés comme abandonnés
WITH eleves_actifs AS (
    -- élèves ayant eu une séance dans les 60 derniers jours
    SELECT DISTINCT r.client_id
    FROM seances s
    JOIN reservations r ON r.id = s.reservation_id
    WHERE s.date_seance >= NOW() - INTERVAL '60 days'
      AND s.statut NOT IN ('cancelled', 'no_show')
),
eleves_recus AS (
    -- élèves ayant réussi la conduite
    SELECT DISTINCT client_id
    FROM resultats_examen
    WHERE type_examen = 'conduite'
      AND resultat    = 'reussi'
)
SELECT
    COUNT(*) FILTER (
        WHERE c.id NOT IN (SELECT client_id FROM eleves_actifs)
          AND c.id NOT IN (SELECT client_id FROM eleves_recus)
          AND c.created_at < NOW() - INTERVAL '90 days'
    )                               AS nb_abandonnes,
    COUNT(*) FILTER (
        WHERE c.created_at < NOW() - INTERVAL '90 days'
    )                               AS nb_eleves_eligibles,
    ROUND(
        COUNT(*) FILTER (
            WHERE c.id NOT IN (SELECT client_id FROM eleves_actifs)
              AND c.id NOT IN (SELECT client_id FROM eleves_recus)
              AND c.created_at < NOW() - INTERVAL '90 days'
        ) * 100.0 / NULLIF(
            COUNT(*) FILTER (
                WHERE c.created_at < NOW() - INTERVAL '90 days'
            ), 0
        ), 1
    )                               AS taux_abandon_pct
FROM clients c
WHERE c.active = true;

-- Détail : depuis combien de temps les abandonnés sont inactifs ?
-- (les CTEs ne persistent pas entre requêtes — eleves_recus est redéfinie ici)
WITH eleves_recus AS (
    SELECT DISTINCT client_id
    FROM resultats_examen
    WHERE type_examen = 'conduite'
      AND resultat    = 'reussi'
)
SELECT
    CASE
        WHEN derniere_activite IS NULL              THEN 'Jamais eu de leçon'
        WHEN derniere_activite < NOW() - INTERVAL '6 months' THEN '> 6 mois'
        WHEN derniere_activite < NOW() - INTERVAL '3 months' THEN '3-6 mois'
        ELSE '60-90 jours'
    END                             AS inactivite,
    COUNT(*)                        AS nb_eleves
FROM (
    SELECT
        c.id,
        MAX(s.date_seance) AS derniere_activite
    FROM clients c
    LEFT JOIN reservations r ON r.client_id = c.id
    LEFT JOIN seances s
        ON s.reservation_id = r.id AND s.statut NOT IN ('cancelled', 'no_show')
    WHERE c.id NOT IN (SELECT client_id FROM eleves_recus)
      AND c.created_at < NOW() - INTERVAL '90 days'
    GROUP BY c.id
) sub
GROUP BY 1
ORDER BY MIN(COALESCE(derniere_activite, '1900-01-01'::date));
```

**Affichage dans le dashboard :**

```
┌──────────────────────────────────────────────────────────────────┐
│  Taux d'abandon                                                  │
│                                                                  │
│  18%  des élèves inscrits n'ont plus d'activité                 │
│  (sur les élèves inscrits depuis > 3 mois)                      │
│                                                                  │
│  Répartition des inactifs :                                      │
│  Jamais eu de leçon    ██░░░░░░░░  4 élèves  → contacter d'urgence
│  60-90 jours           █████░░░░░  9 élèves  → relance email    │
│  3-6 mois              ████░░░░░░  7 élèves  → probablement perdus
│  > 6 mois              ██░░░░░░░░  3 élèves  → archiver         │
│                                                                  │
│  [Exporter la liste]  [Envoyer un email de relance]             │
└──────────────────────────────────────────────────────────────────┘
```

Le bouton "Envoyer un email de relance" déclenche un email Resend personnalisé pour chaque élève inactif — avec le nom du moniteur référent et un lien direct vers le planning en ligne.

**Endpoint :**

```java
@GetMapping("/taux-abandon")
public TauxAbandonStat tauxAbandon() {
    return statsService.tauxAbandon();
}

public record TauxAbandonStat(
    int    nbAbandonnes,
    int    nbElevesEligibles,
    double tauxPct,
    List<InactiviteDetail> detail   // répartition par durée d'inactivité
) {}

// Action : relance email d'un seul élève inactif
@PostMapping("/relance-abandon/{clientId}")
public void relancerEleve(@PathVariable UUID clientId) {
    statsService.envoyerEmailRelance(clientId);
}
```

---

## 13.10 Dashboard statistiques — vue finale complète

Avec les 5 KPIs, la page statistiques ressemble à :

```
┌─────────────────────────────────────────────────────────────────────────┐
│  Auto-École Lyon Centre — Statistiques             Période : 12 mois   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐ │
│  │ Taux occupation │  │ Taux réussite   │  │ Taux d'abandon          │ │
│  │                 │  │ conduite        │  │                         │ │
│  │      78%        │  │                 │  │        18%              │ │
│  │   Zone saine ✓  │  │      71%        │  │  23 élèves inactifs    │ │
│  │                 │  │  +14 pts / nat. │  │  [Voir la liste]       │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────────────┘ │
│                                                                         │
│  Inscriptions par mois ────────────────────────────────────────────    │
│  [graphique barres]                                                     │
│                                                                         │
│  Temps moyen pour obtenir le permis ───────────────────────────────    │
│  4 mois 12 jours · [histogramme distribution]                          │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

Les 3 cartes du haut sont les **métriques de pilotage** — le directeur les voit d'un coup d'œil chaque matin. Les 2 blocs du bas sont les **métriques d'analyse** — on les consulte pour comprendre les tendances.

---

