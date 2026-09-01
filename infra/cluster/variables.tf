variable "microk8s_channel" {
  description = "Canal do snap do microk8s instalado quando o binário não existe no host."
  type        = string
  default     = "1.31/stable"
}

variable "microk8s_addons" {
  description = "Addons do microk8s garantidos a cada apply (idempotente). metrics-server é obrigatório para o HPA (k8s/base/hpa.yaml) calcular utilização de CPU/memória."
  type        = list(string)
  default     = ["dns", "hostpath-storage", "metrics-server"]
}

variable "kubeconfig_path" {
  description = "Caminho do kubeconfig gerado; infra/ lê deste mesmo caminho."
  type        = string
  default     = "~/.kube/config"
}

variable "apiserver_host" {
  description = "Host do apiserver gravado no kubeconfig. 127.0.0.1 é estável e está no SAN do certificado do microk8s; troque só ao rodar o Terraform de outra máquina."
  type        = string
  default     = "127.0.0.1"
}
