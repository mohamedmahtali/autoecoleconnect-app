// Types de l'API backend.
// Cible (voir docs/03-stack-technique.md) : générés automatiquement depuis la
// spec OpenAPI de Spring Boot (/v3/api-docs). Écrits à la main en Phase 0.

export interface PingResponse {
  status: string;
  service: string;
  timestamp: string;
}
