variable "kubeconfig_path" {
  description = "Caminho do kubeconfig do cluster — gerado por infra/cluster/ e fonte do provider kubernetes."
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
