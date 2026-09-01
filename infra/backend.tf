terraform {
  # Backend local. O state NÃO pode mais viver dentro do cluster (como no antigo backend
  # "kubernetes"): agora cluster.tf provisiona o microk8s quando ele não existe, e o
  # `terraform init` roda ANTES do `apply` — em uma máquina limpa não haveria cluster para
  # guardar o state. O caminho é injetado no `terraform init` via -backend-config
  # (ver infra/README.md) para ficar em $HOME/.local/state, fora do diretório de checkout,
  # e sobreviver entre execuções do runner self-hosted.
  backend "local" {}
}
