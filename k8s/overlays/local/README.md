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

1. Namespace `tech-challenge` e as dependências (PostgreSQL, Redis e Mailpit)
   provisionados pelo Terraform — ver `/infra`.
2. Secret `tech-challenge-core-secret` criado fora do Git.
3. Addons `dns`, `storage`, `metrics-server` e `registry` habilitados.

Depois do `terraform apply`, crie o Secret da aplicação a partir da raiz do
repositório:

```bash
kubectl -n tech-challenge create secret generic tech-challenge-core-secret \
  --from-literal=jdbc-username=techdev \
  --from-literal=jdbc-password='<mesma senha do Terraform>' \
  --from-literal=mail-username=noreply@tech.local \
  --from-literal=mail-password='' \
  --from-file=jwt-public-key=core/src/main/resources/certs/dev-public.pem \
  --from-file=jwt-private-key=core/src/main/resources/certs/dev-private.pem
```

## Apply

```bash
kubectl kustomize k8s/overlays/local     # revisa o resultado
kubectl apply -k k8s/overlays/local
kubectl -n tech-challenge rollout status deployment/core --timeout=180s
```

A aplicação responde em `http://<ip-do-node>:30080/core` (health em `/core/actuator/health`).

Para acessar a caixa de entrada do Mailpit:

```bash
kubectl -n tech-challenge port-forward service/mailpit 8025:8025
```

Abra `http://localhost:8025`. O endpoint SMTP usado pela aplicação é
`mailpit:1025`.
