---
noteId: "38dac5507f8711f1878859078c773cc2"
tags: []

---

# 6. Modèle de Données

## 6.1 Deux bases de données distinctes

```
┌─────────────────────────────────────┐   ┌──────────────────────────────────┐
│  BDD Control Plane                  │   │  BDD Tenant (par auto-école)     │
│  namespace: platform                │   │  namespace: agence-lyon          │
│  PostgreSQL (CloudNativePG)         │   │  PostgreSQL (CloudNativePG)      │
│                                     │   │                                  │
│  organisations                      │   │  clients                         │
│  tenants                            │   │  moniteurs                       │
│  platform_users                     │   │  directeurs                      │
│  org_metrics                        │   │  forfaits                        │
│  webhook_events                     │   │  voitures                        │
│  provisioning_logs                  │   │  reservations                    │
│                                     │   │  seances                         │
│                                     │   │  paiements                       │
│                                     │   │  client_documents                │
│                                     │   │  moniteur_documents              │
│                                     │   │  moniteur_disponibilites         │
│                                     │   │  voiture_indisponibilites        │
│                                     │   │  notifications_log               │
│                                     │   │  resultats_examen                │
│                                     │   │  articles_blog                   │
│                                     │   │  faq_items                       │
│                                     │   │  centres_examen                  │
└─────────────────────────────────────┘   └──────────────────────────────────┘
         Gérée par Control Plane                  Gérée par le portail tenant
         Liquibase dans le Control Plane           Liquibase dans Spring Boot tenant
         Ne contient aucune donnée métier          Ne contient aucune donnée plateforme
```

---

## 6.2 Schéma — BDD Control Plane

```sql
-- Organisation (gérant + ses auto-écoles)
CREATE TABLE organisations (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nom                     VARCHAR(255) NOT NULL,
    email_gerant            VARCHAR(255) NOT NULL UNIQUE,
    plan                    VARCHAR(50) NOT NULL DEFAULT 'solo',
                            -- solo | pro | groupe | reseau
    stripe_customer_id      VARCHAR(255),
    stripe_subscription_id  VARCHAR(255),
    statut                  VARCHAR(50) NOT NULL DEFAULT 'trial',
                            -- trial | active | suspended | deleted
    trial_ends_at           TIMESTAMP NOT NULL,
    reminder_sent           BOOLEAN NOT NULL DEFAULT false,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Tenant (une auto-école = un namespace K8s)
CREATE TABLE tenants (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID NOT NULL REFERENCES organisations(id),
    slug            VARCHAR(100) NOT NULL UNIQUE,  -- "agence-lyon"
    namespace       VARCHAR(100) NOT NULL UNIQUE,  -- nom namespace K8s
    nom             VARCHAR(255) NOT NULL,
    url             VARCHAR(255) NOT NULL,          -- "lyon.autoeecoleconnect.app"
    statut          VARCHAR(50) NOT NULL DEFAULT 'provisioning',
                    -- provisioning | active | suspended | deleted
    plan            VARCHAR(50) NOT NULL DEFAULT 'solo',
    config          JSONB NOT NULL DEFAULT '{}',
                    -- { logo_url, couleur, stripe_enabled, alma_enabled... }
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    suspended_at    TIMESTAMP,
    deleted_at      TIMESTAMP
);

-- Métriques agrégées par organisation (alimentées par RabbitMQ)
CREATE TABLE org_metrics (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID NOT NULL REFERENCES organisations(id),
    mois            VARCHAR(7) NOT NULL,            -- "2026-07"
    ca_total        DECIMAL(10,2) NOT NULL DEFAULT 0,
    nb_eleves       INTEGER NOT NULL DEFAULT 0,
    nb_reservations INTEGER NOT NULL DEFAULT 0,
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(org_id, mois)
);

-- Idempotence webhooks Stripe
CREATE TABLE webhook_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stripe_event_id VARCHAR(255) NOT NULL UNIQUE,
    event_type      VARCHAR(100) NOT NULL,
    processed_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Audit trail du provisioning
CREATE TABLE provisioning_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    action          VARCHAR(100) NOT NULL,
                    -- provision | suspend | resume | delete
    statut          VARCHAR(50) NOT NULL,
                    -- pending | success | failed
    detail          TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
```

---

## 6.3 Schéma — BDD Tenant (par auto-école)

Chaque auto-école a sa propre PostgreSQL avec ce schéma identique, géré par Liquibase au démarrage de Spring Boot.

```sql
-- Directeurs (administrateurs de l'auto-école)
CREATE TABLE directeurs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nom             VARCHAR(100) NOT NULL,
    prenom          VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(50) NOT NULL DEFAULT 'directeur',
    active          BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Moniteurs
CREATE TABLE moniteurs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nom             VARCHAR(100) NOT NULL,
    prenom          VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    telephone       VARCHAR(20),
    statut          VARCHAR(50) NOT NULL DEFAULT 'pending',
                    -- pending | approved | rejected | inactive
    notes           TEXT,
    active          BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Clients (élèves)
CREATE TABLE clients (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nom                 VARCHAR(100) NOT NULL,
    prenom              VARCHAR(100) NOT NULL,
    email               VARCHAR(255) NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,
    telephone           VARCHAR(20),
    adresse             TEXT,
    notes               TEXT,
    active              BOOLEAN NOT NULL DEFAULT true,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Forfaits de conduite
CREATE TABLE forfaits (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nom             VARCHAR(255) NOT NULL,
    nombre_heure    INTEGER NOT NULL,
    validite        INTEGER NOT NULL,
    unite           VARCHAR(10) NOT NULL,       -- Mois | Jour
    prix            DECIMAL(10,2) NOT NULL,
    conditions      TEXT,
    categorie       VARCHAR(50) NOT NULL,        -- Conduite | Journalier
    transmission    VARCHAR(20),                 -- Manuelle | Automatique
    kilometrage     VARCHAR(20) NOT NULL,        -- illimite | limite
    nb_kilometre    INTEGER,
    carburant       VARCHAR(20) NOT NULL,        -- inclus | non_inclus
    active          BOOLEAN NOT NULL DEFAULT true
);

-- Véhicules
CREATE TABLE voitures (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nom             VARCHAR(100) NOT NULL,
    marque          VARCHAR(100) NOT NULL,
    transmission    VARCHAR(20) NOT NULL,        -- Manuelle | Automatique
    double_commande BOOLEAN NOT NULL DEFAULT false,
    carburant       VARCHAR(50),
    couleur         VARCHAR(50),
    nb_portes       INTEGER,
    nb_passagers    INTEGER,
    air_conditionne BOOLEAN NOT NULL DEFAULT false,
    note            TEXT,
    active          BOOLEAN NOT NULL DEFAULT true
);

-- Réservations (lien client ↔ forfait)
CREATE TABLE reservations (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id                   UUID NOT NULL REFERENCES clients(id),
    forfait_id                  UUID NOT NULL REFERENCES forfaits(id),
    date_debut                  DATE NOT NULL,
    date_fin                    DATE NOT NULL,
    date_reservation            TIMESTAMP NOT NULL DEFAULT NOW(),
    montant                     DECIMAL(10,2) NOT NULL,
    paiement_type               VARCHAR(50),
                                -- stripe | payplug | alma | espece | cheque
                                -- virement | cpf | permis1euro
    paiement_statut             VARCHAR(50) NOT NULL DEFAULT 'pending',
                                -- pending | paid | refunded | failed
    stripe_payment_intent_id    VARCHAR(255),
    statut                      VARCHAR(50) NOT NULL DEFAULT 'pending',
                                -- pending | active | completed | cancelled | expired
    notes                       TEXT,
    active                      BOOLEAN NOT NULL DEFAULT true,
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Séances de conduite
CREATE TABLE seances (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id      UUID NOT NULL REFERENCES reservations(id),
    moniteur_id         UUID REFERENCES moniteurs(id),
    voiture_id          UUID REFERENCES voitures(id),
    date_seance         DATE NOT NULL,
    h_deb               TIME NOT NULL,
    h_fin               TIME NOT NULL,
    statut              VARCHAR(50) NOT NULL DEFAULT 'scheduled',
                        -- scheduled | completed | cancelled | no_show
    validated_client    BOOLEAN NOT NULL DEFAULT false,
    validated_moniteur  BOOLEAN NOT NULL DEFAULT false,
    validated_admin     BOOLEAN NOT NULL DEFAULT false,
    notes               TEXT,
    active              BOOLEAN NOT NULL DEFAULT true,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Paiements en ligne (trace Stripe / PayPlug / Alma)
CREATE TABLE paiements (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id              UUID NOT NULL REFERENCES reservations(id),
    provider                    VARCHAR(50) NOT NULL,
                                -- stripe | payplug | alma
    provider_payment_id         VARCHAR(255) NOT NULL UNIQUE,
    provider_event_id           VARCHAR(255) UNIQUE,    -- idempotence
    amount_cents                INTEGER NOT NULL,
    currency                    VARCHAR(3) NOT NULL DEFAULT 'EUR',
    statut                      VARCHAR(50) NOT NULL,
                                -- pending | succeeded | failed | refunded
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Disponibilités moniteurs (créneaux hebdo + congés)
CREATE TABLE moniteur_disponibilites (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moniteur_id         UUID NOT NULL REFERENCES moniteurs(id),
    type_disponibilite  VARCHAR(20) NOT NULL,   -- weekly_slot | time_off
    jour_semaine        INTEGER,                 -- 0=lundi ... 6=dimanche
    date_debut          DATE,
    date_fin            DATE,
    heure_debut         TIME,
    heure_fin           TIME,
    motif               TEXT,
    active              BOOLEAN NOT NULL DEFAULT true,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indisponibilités véhicules
CREATE TABLE voiture_indisponibilites (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    voiture_id  UUID NOT NULL REFERENCES voitures(id),
    date_debut  DATE NOT NULL,
    date_fin    DATE NOT NULL,
    motif       TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Log des notifications envoyées
CREATE TABLE notifications_log (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    canal               VARCHAR(20) NOT NULL,       -- email | sms | internal
    type_notification   VARCHAR(100) NOT NULL,
    destinataire        VARCHAR(255),
    reference_table     VARCHAR(100),
    reference_id        UUID,
    statut              VARCHAR(20) NOT NULL,        -- queued | sent | failed
    payload             JSONB NOT NULL DEFAULT '{}',
    erreur              TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);
```

---

## 6.4 Gestion des paiements manuels

Les paiements en espèces, chèque, virement et CPF ne passent pas par une API de paiement — ils sont saisis manuellement par le directeur dans l'interface.

```sql
-- Extension de la table reservations pour les paiements manuels
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS
    paiement_reference  VARCHAR(255),   -- n° chèque, référence virement, n° dossier CPF
    caution_reglee      BOOLEAN NOT NULL DEFAULT false,
    date_paiement       DATE;
```

Le statut `paiement_statut = 'paid'` est mis à jour manuellement par le directeur après réception du paiement physique.

---

## 6.5 Entités JPA — Spring Boot

```java
// Exemple d'entité JPA pour Client
@Entity
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false, unique = true)
    private String email;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Reservation> reservations = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    // Pas d'agence_id — chaque BDD tenant n'appartient qu'à une seule auto-école
}
```

**Point clé :** pas de champ `agence_id` dans aucune entité. L'isolation est au niveau K8s (namespace + BDD séparée). Le code reste simple — chaque instance Spring Boot ne voit qu'une seule auto-école.

---

## 🎓 Montée en compétence — Base de données & JPA

### Pourquoi pas de agence_id dans le schéma tenant ?

C'est la question que tout développeur habitué au row-level multi-tenant posera. La réponse :

```
Row-level :  SELECT * FROM clients WHERE agence_id = 'xxx'
              → risque : oublier le WHERE = fuite de données

Namespace :  La BDD de Lyon ne contient que les clients de Lyon
              → SELECT * FROM clients (sans filtre, 100% safe)
              → L'isolation est physique, pas logicielle
```

### Spring Data JPA — les patterns essentiels

```java
// Repository basique
public interface ClientRepository extends JpaRepository<Client, UUID> {
    List<Client> findByActiveTrue();
    Optional<Client> findByEmail(String email);
    boolean existsByEmail(String email);
}

// Requête JPQL personnalisée
@Query("SELECT c FROM Client c WHERE c.active = true ORDER BY c.nom")
List<Client> findAllActiveOrderedByNom();

// Avec pagination
Page<Client> findByActiveTrue(Pageable pageable);
```

### Liquibase — conventions de nommage

```
db/changelog/
├── db.changelog-master.yaml          ← inclut tous les autres
├── v1.0/
│   ├── 001-create-clients.sql        ← toujours préfixer par numéro
│   ├── 002-create-moniteurs.sql
│   └── 003-create-reservations.sql
└── v1.1/
    └── 001-add-cpf-reference.sql     ← nouvelle version = nouveau dossier

Règle d'or : un fichier Liquibase ne se modifie JAMAIS après avoir été appliqué.
Si tu veux changer quelque chose, crée un nouveau fichier de migration.
```

**Ressources :**
- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Liquibase Best Practices](https://docs.liquibase.com/concepts/bestpractices.html)
- [Use the Index, Luke](https://use-the-index-luke.com/) — comprendre les index PostgreSQL (gratuit, excellent)

**Ce que ça t'apporte sur le CV :**
Concevoir un schéma multi-tenant avec isolation physique (pas de agence_id) et expliquer pourquoi c'est plus robuste que le row-level est un signal fort de maturité. Beaucoup de seniors ne font pas cette distinction clairement.

---

