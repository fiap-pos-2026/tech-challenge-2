# Provisionamento do cluster Kubernetes local (microk8s) exigido pelo Tech Challenge
# ("Criar scripts em Terraform para provisionamento do cluster Kubernetes — local ou cloud").
#
# Abordagem: microk8s roda como serviço snap no host (WSL2/Ubuntu), então o Terraform não pode
# criá-lo com o provider "kubernetes" (que já pressupõe um cluster respondendo). Em vez disso,
# um null_resource com local-exec instala e habilita o microk8s de forma idempotente — script
# só age quando algo realmente falta — e escreve o kubeconfig que os providers kubernetes/helm
# (providers.tf) consomem em seguida via depends_on.
#
# Para apontar para um cluster que já existe por outro meio (cloud gerenciado, cluster do CI
# provisionado por outra pipeline, etc.), desligue com manage_microk8s = false: o Terraform passa
# a só se conectar ao kubeconfig existente, sem tentar instalar nada — cobrindo a opção "cloud" do
# mesmo requisito sem duplicar módulos.

resource "null_resource" "microk8s" {
  count = var.manage_microk8s ? 1 : 0

  triggers = {
    # Reexecuta o provisioner se a lista de addons mudar; instalação em si é idempotente
    # (o script confere o que já está pronto antes de agir).
    addons = join(",", var.microk8s_addons)
  }

  provisioner "local-exec" {
    interpreter = ["/bin/bash", "-c"]
    command     = <<-EOT
      set -euo pipefail

      if ! command -v microk8s >/dev/null 2>&1; then
        echo "[microk8s] não encontrado — instalando via snap..."
        sudo snap install microk8s --classic --channel=${var.microk8s_channel}
        sudo usermod -aG microk8s "$USER"
      else
        echo "[microk8s] já instalado — pulando snap install."
      fi

      echo "[microk8s] aguardando cluster ficar pronto..."
      sudo microk8s status --wait-ready --timeout 180

      for addon in ${join(" ", var.microk8s_addons)}; do
        echo "[microk8s] habilitando addon: $addon"
        sudo microk8s enable "$addon"
      done

      mkdir -p "$(dirname ${var.kubeconfig_path})"
      sudo microk8s config > ${var.kubeconfig_path}
      echo "[microk8s] kubeconfig escrito em ${var.kubeconfig_path}"
    EOT
  }
}
