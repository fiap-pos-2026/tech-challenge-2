output "cluster_managed_by_terraform" {
  description = "Sempre true: cluster.tf valida e provisiona (quando ausente) o cluster microk8s a cada apply."
  value       = true
}

output "namespace" {
  description = "Namespace provisionado no microk8s."
  value       = kubernetes_namespace.tech_challenge.metadata[0].name
}

output "postgres_service" {
  description = "Nome do Service do PostgreSQL (usar como JDBC_HOST na app)."
  value       = kubernetes_service.postgres.metadata[0].name
}

output "postgres_port" {
  description = "Porta do Service do PostgreSQL."
  value       = 5432
}

output "db_name" {
  description = "Nome do banco provisionado (usar como JDBC_SERVICENAME na app)."
  value       = var.db_name
}

output "postgres_credentials_secret" {
  description = "Nome do Secret Kubernetes com as credenciais do PostgreSQL (namespace da app)."
  value       = kubernetes_secret.postgres.metadata[0].name
}

output "ghcr_pull_secret" {
  description = "Nome do Secret de pull do GHCR, ou null quando ghcr_pull_token não foi fornecido."
  value       = one(kubernetes_secret.ghcr_pull[*].metadata[0].name)
}

output "redis_service" {
  description = "Nome do Service do Redis (usar como REDIS_HOST na app)."
  value       = kubernetes_service.redis.metadata[0].name
}

output "redis_port" {
  description = "Porta do Service do Redis."
  value       = 6379
}
