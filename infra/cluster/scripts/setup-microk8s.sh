#!/usr/bin/env bash
# Bootstrap do microk8s: instala/sobe o cluster, habilita addons e grava o kubeconfig.
# Chamado pelo Terraform (infra/cluster/cluster.tf). Variáveis via ambiente.
set -euo pipefail

: "${KUBECONFIG_PATH:?KUBECONFIG_PATH is required}"
: "${APISERVER_HOST:?APISERVER_HOST is required}"
: "${MICROK8S_CHANNEL:?MICROK8S_CHANNEL is required}"
: "${MICROK8S_ADDONS:?MICROK8S_ADDONS is required}"

export PATH="$PATH:/snap/bin"

if ! command -v microk8s >/dev/null 2>&1; then
  echo "[microk8s] instalando (canal ${MICROK8S_CHANNEL})..."
  sudo snap install microk8s --classic --channel="${MICROK8S_CHANNEL}"
  sudo usermod -aG microk8s "$(id -un)" || true
else
  sudo snap start microk8s >/dev/null 2>&1 || true
fi

sudo microk8s status --wait-ready --timeout 300

# shellcheck disable=SC2086
for attempt in 1 2 3; do
  if sudo microk8s enable ${MICROK8S_ADDONS}; then
    break
  fi
  sudo microk8s status --wait-ready --timeout 120 || true
  sleep 10
done

mkdir -p "$(dirname "$KUBECONFIG_PATH")"

write_kubeconfig() {
  sudo microk8s config >"$KUBECONFIG_PATH.tmp" 2>/dev/null || return 1
  grep -q 'certificate-authority-data' "$KUBECONFIG_PATH.tmp" || return 1
  sed -i -E "s#server: https://[0-9.]+:16443#server: https://${APISERVER_HOST}:16443#" "$KUBECONFIG_PATH.tmp"
  grep -q "server: https://${APISERVER_HOST}:16443" "$KUBECONFIG_PATH.tmp"
}

ok=0
for _ in $(seq 1 30); do
  if write_kubeconfig &&
    sudo microk8s kubectl --kubeconfig "$KUBECONFIG_PATH.tmp" get --raw='/readyz' >/dev/null 2>&1; then
    ok=1
    break
  fi
  sleep 10
done

if [ "$ok" -ne 1 ]; then
  sudo microk8s refresh-certs --cert ca.crt || true
  sudo microk8s refresh-certs --cert server.crt || true
  sudo microk8s status --wait-ready --timeout 300
  write_kubeconfig
  sudo microk8s kubectl --kubeconfig "$KUBECONFIG_PATH.tmp" get --raw='/readyz' >/dev/null
fi

mv "$KUBECONFIG_PATH.tmp" "$KUBECONFIG_PATH"
chmod 600 "$KUBECONFIG_PATH"
echo "[microk8s] kubeconfig em $KUBECONFIG_PATH (apiserver https://${APISERVER_HOST}:16443)"
