variable "manage_microk8s" {
  description = "Se true (default), o Terraform instala/habilita o microk8s local via null_resource (cluster.tf) antes de provisionar os demais recursos. Desligue para apontar para um cluster que já existe por outro meio (cloud gerenciado, cluster provisionado fora deste módulo) — o Terraform passa a só se conectar via kubeconfig_path."
  type        = bool
  default     = true
}

variable "microk8s_channel" {
  description = "Canal do snap do microk8s a instalar quando manage_microk8s = true."
  type        = string
  default     = "1.31/stable"
}

variable "microk8s_addons" {
  description = "Addons do microk8s a habilitar quando manage_microk8s = true. metrics-server é obrigatório para o HPA (k8s/base/hpa.yaml) calcular utilização de CPU/memória."
  type        = list(string)
  default     = ["dns", "storage", "metrics-server"]
}

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
