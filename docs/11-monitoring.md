---
noteId: "38dac5557f8711f1878859078c773cc2"
tags: []

---

# 11. Monitoring — Stack PLG

## 11.1 Vue d'ensemble

La stack **PLG** (Prometheus + Loki + Grafana) couvre les trois piliers de l'observabilité :

```
┌─────────────────────────────────────────────────────────────────┐
│               Les 3 piliers de l'observabilité                  │
├───────────────┬──────────────────┬──────────────────────────────┤
│  MÉTRIQUES    │  LOGS            │  TRACES                      │
│  Prometheus   │  Loki            │  (Tempo — optionnel Phase 2) │
│               │                  │                              │
│  "combien ?"  │  "qu'est-il      │  "quel chemin a suivi        │
│  CPU, RAM,    │   passé ?"       │   cette requête ?"           │
│  req/s, p99   │  erreurs, events │  latence par service         │
└───────────────┴──────────────────┴──────────────────────────────┘
                        ↓ tout affiché dans
                   ┌──────────────┐
                   │   Grafana    │
                   │  Dashboards  │
                   │  Alerting    │
                   └──────────────┘
```

**Déploiement :** tout dans le namespace `monitoring` via le Helm chart `kube-prometheus-stack` (inclut Prometheus + Alertmanager + Grafana + règles par défaut).

```
namespace: monitoring
  ├── prometheus         (scrape toutes les métriques)
  ├── alertmanager       (routing des alertes → email/Slack)
  ├── grafana            (dashboards)
  ├── loki               (agrégation logs)
  └── promtail           (DaemonSet — collecte logs sur chaque nœud)
```

---

## 11.2 Installation — kube-prometheus-stack + Loki

```bash
# Ajouter les repos Helm
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update

# Installer kube-prometheus-stack (Prometheus + Alertmanager + Grafana)
helm upgrade --install kube-prometheus-stack prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace \
  --values monitoring/kube-prometheus-stack-values.yaml

# Installer Loki + Promtail
helm upgrade --install loki grafana/loki-stack \
  --namespace monitoring \
  --set promtail.enabled=true \
  --set loki.persistence.enabled=true \
  --set loki.persistence.size=20Gi
```

```yaml
# monitoring/kube-prometheus-stack-values.yaml
grafana:
  adminPassword: "${GRAFANA_ADMIN_PASSWORD}"   # depuis Sealed Secret
  ingress:
    enabled: true
    hosts: ["grafana.autoeecoleconnect.app"]
    tls:
      - secretName: wildcard-tls
        hosts: ["grafana.autoeecoleconnect.app"]

prometheus:
  prometheusSpec:
    retention: 30d
    storageSpec:
      volumeClaimTemplate:
        spec:
          resources:
            requests:
              storage: 50Gi

alertmanager:
  config:
    global:
      slack_api_url: "${SLACK_WEBHOOK_URL}"
    route:
      receiver: slack-critical
    receivers:
      - name: slack-critical
        slack_configs:
          - channel: "#alerts-prod"
            text: "{{ range .Alerts }}{{ .Annotations.summary }}\n{{ end }}"
```

---

## 11.3 Métriques Spring Boot — Actuator + Micrometer

Spring Boot expose automatiquement les métriques Prometheus via Actuator :

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health, prometheus, info
  endpoint:
    prometheus:
      enabled: true
  metrics:
    tags:
      tenant: ${TENANT_SLUG}    # label automatique sur toutes les métriques
      app: backend
```

Spring Boot expose alors `/actuator/prometheus` avec des métriques comme :
```
# Requêtes HTTP
http_server_requests_seconds_count{method="POST",uri="/api/v1/reservations",tenant="agence-lyon"}
http_server_requests_seconds_sum{...}

# JVM
jvm_memory_used_bytes{area="heap",tenant="agence-lyon"}
jvm_gc_pause_seconds_count{tenant="agence-lyon"}

# Métriques métier custom
reservations_created_total{tenant="agence-lyon"}
paiements_total{provider="stripe",statut="success",tenant="agence-lyon"}
```

### Métriques métier custom avec Micrometer

```java
@Service
public class ReservationService {

    private final Counter reservationsCounter;
    private final Timer reservationTimer;

    public ReservationService(MeterRegistry registry) {
        this.reservationsCounter = Counter.builder("reservations.created")
            .description("Nombre de réservations créées")
            .register(registry);

        this.reservationTimer = Timer.builder("reservations.creation.duration")
            .description("Temps de création d'une réservation")
            .register(registry);
    }

    public Reservation create(ReservationRequest request) {
        return reservationTimer.record(() -> {
            Reservation reservation = doCreate(request);
            reservationsCounter.increment();
            return reservation;
        });
    }
}
```

---

## 11.4 PodMonitor — Prometheus scrape par tenant

Prometheus doit savoir qu'il faut scraper le backend de chaque tenant. Le chart Helm du tenant crée un `PodMonitor` automatiquement :

```yaml
# helm/portail-tenant/templates/podmonitor.yaml
apiVersion: monitoring.coreos.com/v1
kind: PodMonitor
metadata:
  name: backend-metrics
  namespace: {{ .Values.tenant.slug }}
  labels:
    release: kube-prometheus-stack    # label requis pour être découvert
spec:
  selector:
    matchLabels:
      app: backend
  podMetricsEndpoints:
    - port: http
      path: /actuator/prometheus
      interval: 30s
```

Prometheus découvre automatiquement tous les `PodMonitor` dans tous les namespaces (grâce à `serviceMonitorNamespaceSelector: {}` dans la config Prometheus).

---

## 11.5 Logs — Loki + Promtail

**Promtail** est un DaemonSet (un pod par nœud) qui collecte les logs de tous les containers et les envoie à Loki avec des labels automatiques :

```
Promtail collecte :
  /var/log/pods/agence-lyon_backend-xxx/.../0.log
    → labels : { namespace="agence-lyon", app="backend", pod="backend-xxx" }
    → envoyé à Loki

Requête Loki pour voir les erreurs d'un tenant :
  {namespace="agence-lyon", app="backend"} |= "ERROR"

Requête Loki pour toutes les erreurs 5xx :
  {namespace=~"agence-.*"} | json | status >= 500
```

### Logs structurés Spring Boot → JSON

Pour que Loki puisse filtrer par champ, les logs doivent être en JSON :

```xml
<!-- pom.xml — remplace le format texte par JSON -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

```xml
<!-- src/main/resources/logback-spring.xml -->
<configuration>
  <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
      <customFields>{"tenant":"${TENANT_SLUG}","app":"backend"}</customFields>
    </encoder>
  </appender>

  <root level="INFO">
    <appender-ref ref="JSON" />
  </root>
</configuration>
```

Chaque ligne de log devient :
```json
{
  "timestamp": "2026-07-12T10:23:45.123Z",
  "level": "ERROR",
  "message": "Paiement refusé — fonds insuffisants",
  "tenant": "agence-lyon",
  "app": "backend",
  "reservationId": "a1b2c3",
  "provider": "stripe"
}
```

---

## 11.6 Dashboards Grafana

### Dashboard plateforme (vue super-admin)

```
┌───────────────────────────────────────────────────────────────────┐
│  AutoEcoleConnect — Dashboard Plateforme                                │
├───────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Tenants actifs : 47      Tenants trial : 12     Suspendus : 3   │
│                                                                   │
│  CPU cluster        RAM cluster       Pods running               │
│  ██████░░░░ 58%     ████████░░ 72%    142 / 150                  │
│                                                                   │
│  Requêtes/s (toute la plateforme)      Taux d'erreur global      │
│  ┌────────────────────────────┐        ┌──────────────────────┐  │
│  │ ╭──╮    ╭────╮            │        │ 0.3%  ───────────── │  │
│  │╭╯  ╰────╯    ╰────────────│        │                      │  │
│  └────────────────────────────┘        └──────────────────────┘  │
│                                                                   │
│  Top 5 tenants par charge CPU                                     │
│  agence-marseille   ████████░░  78%                              │
│  agence-bordeaux    ██████░░░░  61%                              │
│  agence-lyon        █████░░░░░  52%                              │
└───────────────────────────────────────────────────────────────────┘
```

### Dashboard par tenant

```
┌───────────────────────────────────────────────────────────────────┐
│  Auto-École Lyon Centre — Métriques juillet 2026                  │
├───────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Réservations créées aujourd'hui : 23                            │
│  Paiements réussis : 18     Paiements échoués : 2               │
│  Élèves connectés (dernière heure) : 7                           │
│                                                                   │
│  Latence API (p50 / p95 / p99)                                   │
│  45ms / 120ms / 340ms                                            │
│                                                                   │
│  Erreurs (dernières 24h)                                         │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  [10:23] ERROR PaiementService — Stripe timeout            │  │
│  │  [08:15] WARN  ReservationService — créneau déjà pris      │  │
│  └────────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────────┘
```

Les dashboards sont versionnés en JSON dans `autoeecoleconnect-infra/monitoring/dashboards/` et chargés automatiquement par Grafana au démarrage (sidecar `grafana-sc-dashboard`).

---

## 11.7 Alertes — règles Prometheus

```yaml
# autoeecoleconnect-infra/monitoring/alerts.yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: autoeecoleconnect-alerts
  namespace: monitoring
spec:
  groups:
    - name: tenant.alerts
      rules:
        # Pod backend planté
        - alert: TenantBackendDown
          expr: |
            kube_deployment_status_replicas_available{namespace=~"agence-.*", deployment="backend"} == 0
          for: 2m
          labels:
            severity: critical
          annotations:
            summary: "Backend {{ $labels.namespace }} est DOWN depuis 2 minutes"
            runbook: "https://wiki.autoeecoleconnect.app/runbooks/backend-down"

        # Taux d'erreur HTTP > 5%
        - alert: HighErrorRate
          expr: |
            rate(http_server_requests_seconds_count{
              namespace=~"agence-.*",
              status=~"5.."
            }[5m])
            /
            rate(http_server_requests_seconds_count{
              namespace=~"agence-.*"
            }[5m]) > 0.05
          for: 5m
          labels:
            severity: warning
          annotations:
            summary: "Taux d'erreur > 5% sur {{ $labels.namespace }}"

        # Mémoire JVM > 85%
        - alert: JvmMemoryHigh
          expr: |
            jvm_memory_used_bytes{area="heap", namespace=~"agence-.*"}
            /
            jvm_memory_max_bytes{area="heap", namespace=~"agence-.*"} > 0.85
          for: 10m
          labels:
            severity: warning
          annotations:
            summary: "Heap JVM > 85% sur {{ $labels.namespace }}"

        # Backup PostgreSQL manqué
        - alert: BackupMissed
          expr: |
            time() - cnpg_backup_last_success_timestamp{namespace=~"agence-.*"} > 90000
          labels:
            severity: critical
          annotations:
            summary: "Backup PostgreSQL manqué sur {{ $labels.namespace }} (> 25h)"
```

---

## 🎓 Montée en compétence — Observabilité

### Les trois piliers — ce que chacun répond

```
Prometheus répond à "est-ce que mon service est en bonne santé ?"
  → CPU, RAM, req/s, taux d'erreur, latence p99

Loki répond à "qu'est-il passé au moment de l'incident ?"
  → logs filtrables par tenant, par niveau, par timestamp

Grafana est l'interface unifiée : dashboards + alertes + exploration
  → on ne regarde jamais Prometheus ou Loki directement en prod
```

### PromQL — les requêtes essentielles

```promql
# Taux de requêtes HTTP sur un tenant (req/s sur 5 min)
rate(http_server_requests_seconds_count{namespace="agence-lyon"}[5m])

# Latence p99 (99% des requêtes répondent en moins de X ms)
histogram_quantile(0.99,
  rate(http_server_requests_seconds_bucket{namespace="agence-lyon"}[5m])
)

# Taux d'erreur HTTP 5xx
rate(http_server_requests_seconds_count{namespace="agence-lyon", status=~"5.."}[5m])
/
rate(http_server_requests_seconds_count{namespace="agence-lyon"}[5m])

# RAM utilisée par namespace
sum(container_memory_working_set_bytes{namespace=~"agence-.*"}) by (namespace)
```

### LogQL — filtrer les logs Loki

```logql
# Toutes les erreurs du backend agence-lyon
{namespace="agence-lyon", app="backend"} |= "ERROR"

# Erreurs de paiement sur tous les tenants
{namespace=~"agence-.*"} | json | message =~ "(?i)paiement.*erreur|stripe.*timeout"

# Requêtes lentes (> 1s) dans les logs JSON
{namespace="agence-lyon"} | json | duration > 1000
```

### RED Method — la méthode pour monitorer un service

Une approche simple pour savoir quoi mesurer sur chaque service :

```
R — Rate      : combien de requêtes par seconde ?
E — Errors    : quel pourcentage échoue ?
D — Duration  : combien de temps prend chaque requête (p50, p95, p99) ?

Ces 3 métriques suffisent pour détecter 90% des incidents.
```

**Ressources :**
- [kube-prometheus-stack](https://github.com/prometheus-community/helm-charts/tree/main/charts/kube-prometheus-stack) — chart Helm tout-en-un
- [PromQL cheatsheet](https://promlabs.com/promql-cheat-sheet/)
- [Loki LogQL](https://grafana.com/docs/loki/latest/query/)
- [Micrometer — métriques Spring Boot](https://micrometer.io/docs/registry/prometheus)
- [RED Method](https://grafana.com/blog/2018/08/02/the-red-method-how-to-instrument-your-services/)

**Ce que ça t'apporte sur le CV :**
Savoir instrumenter une application Spring Boot avec Micrometer, configurer des PodMonitor pour la découverte automatique, écrire des PrometheusRules pour les alertes critiques et lire un dashboard Grafana en production — c'est le socle de tout rôle SRE ou Platform Engineer. La plupart des candidats ont "utilisé Grafana" ; très peu ont configuré la stack de zéro dans K8s.

---

