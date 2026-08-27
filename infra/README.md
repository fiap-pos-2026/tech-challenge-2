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
| E-mail local | `kubernetes_deployment` + `kubernetes_service` | `mailpit` | Imagem `axllent/mailpit:latest`; SMTP na porta `1025` e interface web na `8025` |

Os nomes dos Services (`postgres`, `redis`, `mailpit`) foram escolhidos para casar exatamente com
`JDBC_HOST`, `REDIS_HOST` e `MAIL_HOST` já definidos em `k8s/base/configmap.yaml` — nenhuma variável
de ambiente da aplicação precisa mudar para consumir esta infra.

## Pré-requisitos

1. **Terraform** ≥ 1.6 instalado.
2. **microk8s** já instalado e rodando (fora do escopo deste módulo).
3. Addons `dns`, `storage` e `metrics-server` habilitados no microk8s.
4. **kubeconfig** do microk8s disponível localmente, por exemplo:

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
  --from-literal=mail-username=noreply@tech.local \
  --from-literal=mail-password='' \
  --from-file=jwt-public-key=../core/src/main/resources/certs/dev-public.pem \
  --from-file=jwt-private-key=../core/src/main/resources/certs/dev-private.pem
```

As chaves `dev-*` servem apenas para validação local. Não use essas chaves em produção.
Em seguida, aplique o Kustomize (`k8s/overlays/local`) — ver
`k8s/overlays/local/README.md`.

O Mailpit não exige autenticação SMTP. O `mail-username` é usado apenas como
endereço de remetente pela aplicação. Para visualizar as mensagens:

```bash
kubectl -n tech-challenge port-forward service/mailpit 8025:8025
```

Acesse `http://localhost:8025`.

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

Isso remove namespace, PostgreSQL, Redis e Mailpit do cluster — **não afeta o microk8s em si**.
