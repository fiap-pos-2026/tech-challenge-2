# Infraestrutura (`/infra`) — Terraform sobre o microk8s

Provisiona, via Terraform, os recursos de apoio necessários para a aplicação `core` rodar no
cluster **microk8s** local (WSL2) já instalado no time. **Este módulo não instala nem configura o
microk8s** — ele assume um cluster acessível via kubeconfig e provisiona somente recursos sobre
esse cluster.

## Recursos criados

| Recurso | Tipo Terraform | Nome no cluster | Observação |
| ------- | -------------- | ---------------- | ---------- |
| Namespace da aplicação | `kubernetes_namespace` | `tech-challenge` (var `namespace`) | Mesmo namespace usado pelo Kustomize em `/k8s` |
| Credenciais do PostgreSQL | `kubernetes_secret` | `postgres-credentials` | `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` |
| Banco de dados | `kubernetes_stateful_set` + `kubernetes_service` | `postgres` | Imagem `postgres:18.4-alpine` (mesma do `docker-compose.yml`); volume persistente |
| Cache / rate limiting | `kubernetes_deployment` + `kubernetes_service` | `redis` | Imagem `redis:7-alpine` (mesma do `docker-compose.yml`); sem senha, sem persistência (`--save ""`) |

Os nomes dos Services (`postgres`, `redis`) foram escolhidos para casar exatamente com
`JDBC_HOST` e `REDIS_HOST` já definidos em `k8s/base/configmap.yaml` — nenhuma variável de ambiente
da aplicação precisa mudar para consumir esta infra.

> **Desvio de design**: os charts Helm `bitnami/postgresql` e `bitnami/redis` foram avaliados, mas os
> charts Bitnami atuais dependem do registry OCI `registry-1.docker.io/bitnamicharts` e parte das
> versões mais recentes exige assinatura ("Bitnami Secure Images"). Para não acoplar o `terraform
> apply` a credenciais externas fora do controle deste desafio, os recursos foram declarados como
> manifests equivalentes via provider `kubernetes` (mesma opção prevista no design: "Helm ou
> manifest equivalente"). O provider `helm` (`infra/providers.tf`, T13) permanece disponível para uso
> futuro caso o time volte a usar charts de terceiros.

## Pré-requisitos

1. **Terraform** ≥ 1.6 instalado.
2. **microk8s** já instalado e rodando (fora do escopo deste módulo).
3. **kubeconfig** do microk8s disponível localmente, por exemplo:

   ```bash
   microk8s config > ~/.kube/config
   ```

   Ou, se você já tem outros contexts no `~/.kube/config`, gere um arquivo separado e aponte a
   variável `kubeconfig_path` (ver abaixo).

## Variáveis

| Variável | Default | Descrição |
| -------- | ------- | ---------- |
| `kubeconfig_path` | `~/.kube/config` | Caminho do kubeconfig do microk8s |
| `kube_context` | `""` (context corrente) | Nome do context, se houver múltiplos clusters no kubeconfig |
| `namespace` | `tech-challenge` | Namespace onde os recursos são criados |
| `db_name` | `techbase` | Nome do banco PostgreSQL |
| `db_username` | `techdev` | Usuário do PostgreSQL |
| `db_password` | *(sem default — obrigatório)* | Senha do PostgreSQL; nunca commitar o valor real |
| `postgres_image` | `postgres:18.4-alpine` | Imagem do PostgreSQL |
| `postgres_storage_size` | `2Gi` | Tamanho do volume persistente do PostgreSQL |
| `redis_image` | `redis:7-alpine` | Imagem do Redis |

Copie o exemplo e ajuste os valores reais (o arquivo `terraform.tfvars` é ignorado pelo git):

```bash
cp infra/terraform.tfvars.example infra/terraform.tfvars
# edite infra/terraform.tfvars e defina db_password com uma senha real
```

## Passos

```bash
cd infra
terraform init
terraform plan -var-file=terraform.tfvars
terraform apply -var-file=terraform.tfvars
```

Após o `apply`, crie o Secret real da aplicação com a **mesma senha** usada em `db_password`
(ver `k8s/base/secret.example.yaml`):

```bash
kubectl -n tech-challenge create secret generic tech-challenge-core-secret \
  --from-literal=jdbc-username=techdev \
  --from-literal=jdbc-password='<mesma senha de db_password>' \
  --from-literal=mail-username='' \
  --from-literal=mail-password='' \
  --from-file=jwt-public-key=./public.pem \
  --from-file=jwt-private-key=./private.pem
```

Em seguida, aplique o Kustomize (`k8s/overlays/local`) — ver `k8s/overlays/local/README.md`.

## Outputs

| Output | Descrição |
| ------ | --------- |
| `namespace` | Namespace provisionado |
| `postgres_service` | Nome do Service do PostgreSQL (`postgres`) |
| `postgres_port` | Porta do PostgreSQL (`5432`) |
| `db_name` | Nome do banco (`techbase`) |
| `postgres_credentials_secret` | Nome do Secret Kubernetes com as credenciais do PostgreSQL |
| `redis_service` | Nome do Service do Redis (`redis`) |
| `redis_port` | Porta do Redis (`6379`) |

```bash
terraform output
```

## Destruir os recursos

```bash
terraform destroy -var-file=terraform.tfvars
```

Isso remove namespace, PostgreSQL e Redis do cluster — **não afeta o microk8s em si**.
