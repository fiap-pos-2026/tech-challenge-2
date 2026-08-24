# Terraform NÃO instala nem provisiona o cluster microk8s — apenas se conecta a um
# cluster já existente via kubeconfig. Ver `infra/README.md` para o pré-requisito.

variable "kubeconfig_path" {
  description = "Caminho do kubeconfig do microk8s (gerado com `microk8s config > ~/.kube/config-microk8s`)."
  type        = string
  default     = "~/.kube/config"
}

variable "kube_context" {
  description = "Nome do context do kubeconfig a usar. Vazio usa o context corrente."
  type        = string
  default     = ""
}

provider "kubernetes" {
  config_path    = var.kubeconfig_path
  config_context = var.kube_context != "" ? var.kube_context : null
}

provider "helm" {
  kubernetes {
    config_path    = var.kubeconfig_path
    config_context = var.kube_context != "" ? var.kube_context : null
  }
}
