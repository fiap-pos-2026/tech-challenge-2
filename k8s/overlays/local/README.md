# Overlay `local` (microk8s / WSL2)

Aplica a base do `/k8s` no cluster microk8s local, com a imagem publicada no GitHub Container
Registry (GHCR) e o Service exposto via NodePort.

## Imagem

| Item | Valor |
| ---- | ----- |
| Registry | `ghcr.io` |
| Imagem | `ghcr.io/fiap-pos-2026/tech-challenge-core` |
| Tag do CI/CD | SHA do commit |
| Tag manual | `latest` |

O pacote `fiap-pos-2026/tech-challenge-core` precisa estar público no GHCR. O workflow de CI/CD
publica automaticamente uma tag imutável com o SHA do commit e atualiza a tag `latest`.

## Pré-requisitos no cluster

1. Namespace `tech-challenge` e as dependências (PostgreSQL, Redis e Mailpit)
   provisionados pelo Terraform — ver `/infra`.
2. Secret `tech-challenge-core-secret` criado fora do Git.
3. Addons `dns`, `storage` e `metrics-server` habilitados.

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
# Para uma execução manual, use a tag publicada desejada.
kubectl -n tech-challenge set image deployment/core \
  core=ghcr.io/fiap-pos-2026/tech-challenge-core:latest
kubectl -n tech-challenge rollout status deployment/core --timeout=180s
```

No workflow, a tag `latest` aplicada pelo overlay é substituída pela tag SHA do commit antes da
validação do rollout. Isso garante que o cluster execute exatamente a imagem validada e publicada
pela pipeline.

A aplicação responde em `http://<ip-do-node>:30080/core` (health em `/core/actuator/health`).

Para acessar a caixa de entrada do Mailpit:

```bash
kubectl -n tech-challenge port-forward service/mailpit 8025:8025
```

Abra `http://localhost:8025`. O endpoint SMTP usado pela aplicação é
`mailpit:1025`.
