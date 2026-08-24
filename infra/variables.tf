variable "namespace" {
  description = "Namespace do microk8s onde a app e as dependências de dados são provisionadas."
  type        = string
  default     = "tech-challenge"
}

variable "db_name" {
  description = "Nome do banco PostgreSQL. Deve casar com JDBC_SERVICENAME esperado pela app."
  type        = string
  default     = "techbase"
}

variable "db_username" {
  description = "Usuário do PostgreSQL. Deve casar com JDBC_USERNAME esperado pela app."
  type        = string
  default     = "techdev"
}

variable "db_password" {
  description = "Senha do PostgreSQL. Sem default de propósito — forneça via terraform.tfvars (fora do git) ou TF_VAR_db_password. Deve casar com JDBC_PASSWORD no Secret da app (k8s/base/secret.example.yaml)."
  type        = string
  sensitive   = true
}

variable "postgres_image" {
  description = "Imagem do PostgreSQL — mesma tag usada no docker-compose.yml para paridade dev/prd."
  type        = string
  default     = "postgres:18.4-alpine"
}

variable "postgres_storage_size" {
  description = "Tamanho do volume persistente do PostgreSQL."
  type        = string
  default     = "2Gi"
}

variable "redis_image" {
  description = "Imagem do Redis — mesma tag usada no docker-compose.yml para paridade dev/prd."
  type        = string
  default     = "redis:7-alpine"
}
