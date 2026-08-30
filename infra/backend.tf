terraform {
  # Mantém o estado no próprio microk8s para que o CI e as execuções locais
  # compartilhem a mesma fonte de verdade, sem versionar terraform.tfstate.
  backend "kubernetes" {
    secret_suffix = "tech-challenge"
    namespace     = "default"
    config_path   = "~/.kube/config"
  }
}
