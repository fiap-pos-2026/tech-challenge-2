# Provisionamento do cluster Kubernetes local (microk8s) exigido pelo Tech Challenge
# ("Criar scripts em Terraform para provisionamento do cluster Kubernetes — local ou cloud").
#
# Não há mais flag para ligar/desligar (o antigo `manage_microk8s`). A cada `terraform apply`
# este recurso VALIDA o ambiente e age apenas no que falta:
#   1. instala o microk8s (snap) se o binário não existir no host;
#   2. sobe o cluster se ele não estiver respondendo;
#   3. garante os addons exigidos pela app (o `microk8s enable` é idempotente);
#   4. (re)escreve o kubeconfig que os providers kubernetes/helm (providers.tf) consomem a
#      seguir via depends_on.
#
# Ou seja: se o cluster já existe, todos os passos viram no-op; se não existe, ele é criado —
# "se não existir cluster, cria". Como o local-exec não é interativo (no CI menos ainda), os
# comandos `sudo` abaixo exigem sudo SEM SENHA para snap/microk8s/usermod no host do runner —
# ver SETUP-WSL.md na raiz do repositório.
#
# Para usar um cluster gerenciado na cloud (EKS/GKE/AKS), substitua este arquivo pelo módulo do
# provider correspondente e aponte kubeconfig_path/kube_context para o kubeconfig desse cluster;
# o restante do módulo (main.tf) não muda.

resource "null_resource" "microk8s" {
  # Reexecuta o provisioner em todo apply: o script é idempotente e rápido quando o cluster já
  # está pronto, e assim um cluster parado ou removido é reconstruído sem intervenção manual.
  triggers = {
    always_run = timestamp()
    addons     = join(",", var.microk8s_addons)
    channel    = var.microk8s_channel
  }

  provisioner "local-exec" {
    interpreter = ["/bin/bash", "-c"]
    command     = <<-EOT
      set -euo pipefail

      # O shell do runner é `bash --noprofile --norc`, que não carrega /etc/profile —
      # garante /snap/bin no PATH para achar `snap` e `microk8s` sem depender disso.
      export PATH="$PATH:/snap/bin"

      KUBECONFIG_PATH="${pathexpand(var.kubeconfig_path)}"

      # 1. Instala o microk8s se ainda não houver binário no host.
      if ! command -v microk8s >/dev/null 2>&1; then
        echo "[microk8s] não encontrado — instalando via snap (canal ${var.microk8s_channel})..."
        sudo snap install microk8s --classic --channel=${var.microk8s_channel}
        sudo usermod -aG microk8s "$(id -un)" || true
      else
        echo "[microk8s] binário já presente — pulando instalação."
        # `snap start` (nível systemd) em vez de `microk8s start`, que chama snapctl e
        # falha fora do contexto do snap em alguns ambientes. Idempotente.
        sudo snap start microk8s >/dev/null 2>&1 || true
      fi

      # 2. Espera o cluster ficar realmente pronto. Recém-instalado leva ~1-2 min.
      sudo microk8s status --wait-ready --timeout 300

      # 3. Garante os addons (uma chamada só; `enable` é idempotente). Logo após o install a
      #    habilitação pode falhar de forma transitória — tenta de novo sem abortar o script.
      for attempt in 1 2 3; do
        if sudo microk8s enable ${join(" ", var.microk8s_addons)}; then
          break
        fi
        echo "[microk8s] 'enable' falhou (tentativa $attempt/3) — aguardando cluster e repetindo..."
        sudo microk8s status --wait-ready --timeout 120 || true
        sleep 10
      done

      # 4. (Re)escreve o kubeconfig consumido pelos providers kubernetes/helm. O cluster-agent
      #    ainda pode estar subindo logo após habilitar addons, então tenta até vir um arquivo
      #    válido em vez de morrer no primeiro código != 0.
      mkdir -p "$(dirname "$KUBECONFIG_PATH")"
      for attempt in $(seq 1 30); do
        if sudo microk8s config > "$KUBECONFIG_PATH.tmp" 2>/dev/null && grep -q 'server:' "$KUBECONFIG_PATH.tmp"; then
          mv "$KUBECONFIG_PATH.tmp" "$KUBECONFIG_PATH"
          chmod 600 "$KUBECONFIG_PATH"
          echo "[microk8s] kubeconfig escrito em $KUBECONFIG_PATH"
          exit 0
        fi
        echo "[microk8s] aguardando kubeconfig ($attempt/30)..."
        sleep 5
      done

      echo "[microk8s] ERRO: kubeconfig não ficou pronto a tempo" >&2
      exit 1
    EOT
  }
}
