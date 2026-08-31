# Infraestrutura (`/infra`) — Terraform: cluster + recursos de apoio

Provisiona, via Terraform, o cluster Kubernetes local (**microk8s**, WSL2/Ubuntu) e os recursos de
apoio necessários para a aplicação `core` — namespace, PostgreSQL, Redis e Mailpit. Um único
`terraform apply` cobre as duas pontas do requisito "provisionamento do cluster Kubernetes (local
ou cloud)": o cluster em si (`cluster.tf`) e o banco de dados (`main.tf`).

## Recursos criados

| Recurso | Tipo Terraform | Nome no cluster | Observação |
| ------- | -------------- | ---------------- | ---------- |
| Cluster Kubernetes | `null_resource` + `local-exec` (`cluster.tf`) | microk8s (serviço snap no host) | A cada apply valida o ambiente: instala o snap se ausente, sobe o cluster se parado, garante os addons `dns`/`hostpath-storage`/`metrics-server` e (re)gera o kubeconfig. Idempotente — pula o que já está pronto. Não há flag para desligar; para cloud, ver abaixo |
| Namespace da aplicação | `kubernetes_namespace` | `tech-challenge` (var `namespace`) | Mesmo namespace usado pelo Kustomize em `/k8s` |
| Credenciais do PostgreSQL | `kubernetes_secret` | `postgres-credentials` | `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` |
| Pull da imagem privada no GHCR | `kubernetes_secret` (`kubernetes.io/dockerconfigjson`) | `ghcr-pull` | Criado só quando `ghcr_pull_token` é fornecido; referenciado por `imagePullSecrets` em `k8s/base/deployment.yaml`. No CI vem do secret `GHCR_PULL_TOKEN` (PAT `read:packages`) |
| Banco de dados | `kubernetes_stateful_set` + `kubernetes_service` | `postgres` | Imagem `postgres:18.4-alpine` (mesma do `docker-compose.yml`); volume persistente |
| Cache / rate limiting | `kubernetes_deployment` + `kubernetes_service` | `redis` | Imagem `redis:7-alpine` (mesma do `docker-compose.yml`); sem senha, sem persistência (`--save ""`) |
| E-mail local | `kubernetes_deployment` + `kubernetes_service` | `mailpit` | Imagem `axllent/mailpit:latest`; SMTP na porta `1025` e interface web na `8025` |

Os nomes dos Services (`postgres`, `redis`, `mailpit`) foram escolhidos para casar exatamente com
`JDBC_HOST`, `REDIS_HOST` e `MAIL_HOST` já definidos em `k8s/base/configmap.yaml` — nenhuma variável
de ambiente da aplicação precisa mudar para consumir esta infra.

### Por que microk8s via `local-exec` em vez de um provider de cluster

microk8s roda como serviço no próprio host (WSL2/Ubuntu) — não é algo que o provider `kubernetes`
cria, pois esse provider já pressupõe um cluster respondendo. Por isso `cluster.tf` usa um
`null_resource` com `local-exec` que, **a cada `apply`**, valida o ambiente e age só no que
falta: instala o snap do microk8s quando ausente, sobe o cluster se estiver parado, garante os
addons e (re)escreve o kubeconfig que os providers `kubernetes`/`helm` (`providers.tf`) passam a
usar. O script é idempotente — reexecuta sem efeito colateral se o cluster já existir — então
cobre tanto o primeiro `apply` numa máquina limpa quanto as execuções seguintes do CI. Não há
mais a flag `manage_microk8s`: "se não existir cluster, cria".

### Cobrindo a opção "cloud" do mesmo requisito

Para apontar para um cluster gerenciado (EKS/GKE/AKS), substitua `cluster.tf` pelo módulo do
provider correspondente e aponte `kubeconfig_path`/`kube_context` para o kubeconfig desse
cluster. Namespace, banco e demais recursos deste módulo (`main.tf`) continuam exatamente iguais.

## Pré-requisitos

1. **Terraform** ≥ 1.6 instalado.
2. Host Ubuntu/WSL2 com **snap** disponível (e systemd ativo no WSL).
3. `sudo` disponível para snap/microk8s/usermod. Ver "Autenticação do sudo" abaixo — no CI
   isso significa **sudo sem senha** para esses binários (o `local-exec` não é interativo).

### Autenticação do sudo

`cluster.tf` roda `sudo snap install microk8s`, `sudo microk8s enable ...` etc. dentro de um
`local-exec`, que não é interativo — não há como o Terraform "parar" no meio do apply e pedir uma
senha.

- **Localmente (você, no WSL, no seu terminal)**: autentique o `sudo` *antes* de rodar o
  Terraform, no mesmo terminal, com `sudo -v`. Isso guarda a autorização no cache do sudo
  (15 min por padrão no Ubuntu); os comandos `sudo` dentro do `local-exec` herdam essa
  autenticação. Renove com `sudo -v` em outro terminal se o apply demorar mais que isso.

- **No runner self-hosted do CI**: o `terraform apply` do CI roda `cluster.tf` de forma
  totalmente automatizada, então o usuário do runner precisa de **sudo sem senha** restrito a
  `snap`, `microk8s` e `usermod`. Como configurar isso (sudoers drop-in), além de systemd no
  WSL, runner como serviço e boot automático, está em **`SETUP-WSL.md`** na raiz do repositório.

## Variáveis

| Variável | Default | Descrição |
| -------- | ------- | ---------- |
| `microk8s_channel` | `1.31/stable` | Canal do snap do microk8s instalado por `cluster.tf` quando o binário não existe |
| `microk8s_addons` | `["dns", "hostpath-storage", "metrics-server"]` | Addons garantidos a cada apply; `metrics-server` é obrigatório para o HPA (`k8s/base/hpa.yaml`) |
| `kubeconfig_path` | `~/.kube/config` | Caminho do kubeconfig — destino de `microk8s config` em `cluster.tf` e fonte dos providers |
| `kube_context` | `""` (context corrente) | Nome do context, se houver múltiplos clusters no kubeconfig |
| `namespace` | `tech-challenge` | Namespace onde os recursos são criados |
| `db_name` | `techbase` | Nome do banco PostgreSQL |
| `db_username` | `techdev` | Usuário do PostgreSQL |
| `db_password` | *(sem default — obrigatório)* | Senha do PostgreSQL; nunca commitar o valor real |
| `postgres_image` | `postgres:18.4-alpine` | Imagem do PostgreSQL |
| `postgres_storage_size` | `2Gi` | Tamanho do volume persistente do PostgreSQL |
| `redis_image` | `redis:7-alpine` | Imagem do Redis |
| `ghcr_username` | `johncgo` | Owner do pacote da imagem no GHCR; vira `docker-username` no Secret `ghcr-pull`. No CI vem de `TF_VAR_ghcr_username = github.repository_owner`; o default cobre só execuções locais |
| `ghcr_pull_token` | *(vazio)* | Token GitHub com escopo `read:packages`. Vazio = não cria o Secret `ghcr-pull` (imagem pública ou build local). No CI vem de `TF_VAR_ghcr_pull_token` (secret `GHCR_PULL_TOKEN`); nunca commitar o valor real |

Copie o exemplo e ajuste os valores reais (o arquivo `terraform.tfvars` é ignorado pelo git):

```bash
cp infra/terraform.tfvars.example infra/terraform.tfvars
# edite infra/terraform.tfvars e defina db_password com uma senha real
```

## Passos

```bash
cd infra
terraform init -backend-config="path=$HOME/.local/state/tech-challenge/terraform.tfstate"
terraform plan -var-file=terraform.tfvars
terraform apply -var-file=terraform.tfvars
```

## Estado do Terraform (backend local)

O state usa o backend `local` num caminho **fora do diretório de checkout**
(`$HOME/.local/state/tech-challenge/terraform.tfstate`), passado no `terraform init` via
`-backend-config`. Ele não pode mais viver dentro do cluster (backend `kubernetes`): agora
`cluster.tf` provisiona o microk8s quando ele não existe, e o `terraform init` roda **antes** do
`apply` — numa máquina limpa não haveria cluster para guardar o state.

No runner self-hosted persistente, esse arquivo sobrevive entre execuções. O CI usa o mesmo
caminho (ver `.github/workflows/ci-cd.yml`). Para partilhar state entre máquinas diferentes,
troque para um backend remoto (`s3`, `gcs`, `azurerm`) via `-backend-config`.

Se você tinha state no antigo backend Kubernetes, migre uma única vez:

```bash
cd infra
terraform init -migrate-state -force-copy \
  -backend-config="path=$HOME/.local/state/tech-challenge/terraform.tfstate"
```

Se não houver state anterior e os recursos já existirem no cluster, importe-os antes do primeiro
`apply`:

```bash
terraform import kubernetes_namespace.tech_challenge tech-challenge
terraform import kubernetes_secret.postgres tech-challenge/postgres-credentials
terraform import kubernetes_stateful_set.postgres tech-challenge/postgres
terraform import kubernetes_service.postgres tech-challenge/postgres
terraform import kubernetes_deployment.redis tech-challenge/redis
terraform import kubernetes_service.redis tech-challenge/redis
terraform import kubernetes_deployment.mailpit tech-challenge/mailpit
terraform import kubernetes_service.mailpit tech-challenge/mailpit
terraform import 'kubernetes_secret.ghcr_pull[0]' tech-challenge/ghcr-pull
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
| `cluster_managed_by_terraform` | Sempre `true` — `cluster.tf` valida/provisiona o microk8s a cada apply |
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

Isso remove namespace, PostgreSQL, Redis e Mailpit do cluster. **Não desinstala o microk8s** — o
`null_resource.microk8s` só instala/valida no `apply` e é deliberadamente sem efeito no
`destroy`, para não arriscar remover o cluster e seu estado por engano. Para desinstalar o
microk8s (por exemplo, para provar o provisionamento do zero pelo CI), faça manualmente com
`sudo snap remove microk8s --purge` — ver `SETUP-WSL.md`.
