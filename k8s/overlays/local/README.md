# Overlay `local` (microk8s / WSL2)

Aplica a base do `/k8s` no cluster microk8s local, com a imagem publicada no registry embutido do
microk8s e o Service exposto via NodePort.

## Imagem

| Item | Valor |
| ---- | ----- |
| Registry | `localhost:32000` (addon `registry` do microk8s) |
| Imagem | `localhost:32000/tech-challenge-core` |
| Tag | `local` |

```bash
microk8s enable registry            # habilita o registry em localhost:32000
docker build -f core/Dockerfile -t localhost:32000/tech-challenge-core:local .
docker push localhost:32000/tech-challenge-core:local
```

## Pré-requisitos no cluster

1. Namespace `tech-challenge` e as dependências (PostgreSQL, Redis, Mailpit) provisionados — ver `/infra`.
2. Secret com os valores reais criado fora do git (ver `k8s/base/secret.example.yaml`).
3. Addons `metrics-server` (para o HPA) e `registry` habilitados.

## Apply

```bash
kubectl kustomize k8s/overlays/local     # revisa o resultado
kubectl apply -k k8s/overlays/local
```

A aplicação responde em `http://<ip-do-node>:30080/core` (health em `/core/actuator/health`).
