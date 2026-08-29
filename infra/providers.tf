# Quando manage_microk8s = true (default), o kubeconfig referenciado abaixo é gerado pelo
# próprio Terraform em cluster.tf (`microk8s config > kubeconfig_path`) antes destes providers
# serem usados. Quando manage_microk8s = false, ele deve apontar para um kubeconfig já existente
# de um cluster provisionado por outro meio (local ou cloud). Ver `infra/README.md`.

variable "kubeconfig_path" {
  description = "Caminho do kubeconfig do cluster. Com manage_microk8s = true é o destino de `microk8s config`; com manage_microk8s = false, deve apontar para um kubeconfig já existente."
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
