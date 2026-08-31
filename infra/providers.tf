# O kubeconfig referenciado abaixo é gerado/atualizado pelo próprio Terraform em cluster.tf
# (`microk8s config > kubeconfig_path`) antes destes providers serem usados. Para um cluster
# gerenciado na cloud, substitua cluster.tf e aponte este caminho (e kube_context) para o
# kubeconfig desse cluster. Ver `infra/README.md`.

variable "kubeconfig_path" {
  description = "Caminho do kubeconfig do cluster — destino de `microk8s config` em cluster.tf e fonte dos providers kubernetes/helm."
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
