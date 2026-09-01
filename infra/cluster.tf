# Provisionamento do cluster Kubernetes local (microk8s) exigido pelo Tech Challenge
# ("Criar scripts em Terraform para provisionamento do cluster Kubernetes — local ou cloud").
#
# Não há mais flag para ligar/desligar (o antigo `manage_microk8s`). A cada `terraform apply`
# este recurso VALIDA o ambiente e age apenas no que falta:
#   1. instala o microk8s (snap) se o binário não existir no host;
#   2. sobe o cluster se ele não estiver respondendo;
#   3. garante os addons exigidos pela app (o `microk8s enable` é idempotente);
#   4. (re)escreve o kubeconfig que o provider kubernetes (providers.tf) consome a
#      seguir via depends_on.
#
# Ou seja: se o cluster já existe, todos os passos viram no-op; se não existe, ele é criado —
# "se não existir cluster, cria". Como o local-exec não é interativo (no CI menos ainda), os
# comandos `sudo` abaixo exigem sudo SEM SENHA para snap/microk8s/usermod no host do runner —
# ver a seção "Autenticação do sudo" em infra/README.md.
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

      # 4. Exporta o kubeconfig e só o aceita depois que uma chamada de API REAL com ele
      #    passa. Nos primeiros minutos o microk8s regenera os certificados; um kubeconfig
      #    capturado nessa janela faz o provider kubernetes falhar com "x509: unknown
      #    authority". A validação usa `microk8s kubectl --kubeconfig` (não exige sudo além
      #    do já liberado para o microk8s).
      mkdir -p "$(dirname "$KUBECONFIG_PATH")"

      ok=0
      for attempt in $(seq 1 30); do
        if sudo microk8s config > "$KUBECONFIG_PATH.tmp" 2>/dev/null \
           && grep -q 'certificate-authority-data' "$KUBECONFIG_PATH.tmp" \
           && sudo microk8s kubectl --kubeconfig "$KUBECONFIG_PATH.tmp" get --raw='/readyz' >/dev/null 2>&1; then
          ok=1
          break
        fi
        echo "[microk8s] kubeconfig ainda não valida ($attempt/30)..."
        sleep 10
      done

      if [ "$ok" -ne 1 ]; then
        echo "[microk8s] kubeconfig não validou — forçando refresh-certs..."
        sudo microk8s refresh-certs --cert ca.crt || true
        sudo microk8s refresh-certs --cert server.crt || true
        sudo microk8s status --wait-ready --timeout 300
        sudo microk8s config > "$KUBECONFIG_PATH.tmp"
        sudo microk8s kubectl --kubeconfig "$KUBECONFIG_PATH.tmp" get --raw='/readyz' >/dev/null
      fi

      mv "$KUBECONFIG_PATH.tmp" "$KUBECONFIG_PATH"
      chmod 600 "$KUBECONFIG_PATH"
      echo "[microk8s] kubeconfig validado e escrito em $KUBECONFIG_PATH"
    EOT
  }
}
