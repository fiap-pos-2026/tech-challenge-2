# Infraestrutura (`/infra`) — Terraform: cluster + recursos de apoio

Provisiona, via Terraform, o cluster Kubernetes local (**microk8s**, WSL2/Ubuntu) e os recursos de
apoio da aplicação `core` — namespace, PostgreSQL, Redis e Mailpit. Cobre as duas pontas do
requisito "provisionamento do cluster Kubernetes (local ou cloud)".

## Dois configs Terraform

| Config | State | Provider | Faz |
| ------ | ----- | -------- | --- |
| `infra/cluster/` | `.../tech-challenge/cluster.tfstate` | só `null` | Bootstrap: instala o microk8s se ausente, sobe se parado, garante os addons, escreve o kubeconfig |
| `infra/` (raiz) | `.../tech-challenge/terraform.tfstate` | `kubernetes` | Namespace + PostgreSQL + Redis + Mailpit |

**A ordem importa:** `infra/cluster/` roda **antes** de `infra/`.

### Por que dois configs (e não um `apply` só)

O provider `kubernetes` faz *refresh* de todos os recursos no início de qualquer `plan`/`apply` —
ele abre uma conexão real com a API do cluster (endereço do kubeconfig) para cada `kubernetes_*`
já no state. Num host **sem cluster**, esse refresh falha com `connection refused` **antes** de o
`null_resource` que instala o microk8s ter chance de rodar — o `apply` nunca chega a instalar
nada. Isolar o bootstrap num config sem provider `kubernetes` garante que o `apply` dele sempre
alcance o passo de instalação. Depois, com o cluster no ar, o config de recursos roda normal.

O bootstrap é idempotente: cluster já pronto ⇒ todos os passos viram no-op; cluster ausente ⇒
é criado. "Se não existir cluster, cria."

### O IP do apiserver no kubeconfig

O `microk8s config` grava `server: https://<ip-do-eth0>:16443`, e o IP do `eth0` do WSL2 **muda
a cada restart** — kubeconfig obsoleto, provider falha. O bootstrap reescreve essa linha para
`https://127.0.0.1:16443` (var `apiserver_host`): estável, e `127.0.0.1` está no SAN do
certificado do apiserver do microk8s. Só troque se o Terraform rodar de outra máquina que não o
host do cluster.

### Cobrindo a opção "cloud" do mesmo requisito

Para um cluster gerenciado (EKS/GKE/AKS): ignore `infra/cluster/`, gere o kubeconfig do cluster
gerenciado e aponte `kubeconfig_path`/`kube_context` para ele. `infra/` (namespace, banco, etc.)
continua igual.

## Recursos criados

| Recurso | Config | Tipo Terraform | Nome no cluster | Observação |
| ------- | ------ | -------------- | ---------------- | ---------- |
| Cluster Kubernetes | `infra/cluster/` | `null_resource` + `local-exec` | microk8s (serviço snap no host) | Instala/sobe o microk8s, garante `dns`/`hostpath-storage`/`metrics-server`, escreve o kubeconfig. Idempotente |
| Namespace da aplicação | `infra/` | `kubernetes_namespace` | `tech-challenge` (var `namespace`) | Mesmo namespace usado pelo Kustomize em `/k8s` |
| Credenciais do PostgreSQL | `infra/` | `kubernetes_secret` | `postgres-credentials` | `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` |
| Pull da imagem privada no GHCR | `infra/` | `kubernetes_secret` (`dockerconfigjson`) | `ghcr-pull` | Criado só quando `ghcr_pull_token` é fornecido; referenciado por `imagePullSecrets` em `k8s/base/deployment.yaml` |
| Banco de dados | `infra/` | `kubernetes_stateful_set` + `kubernetes_service` | `postgres` | Imagem `postgres:18.4-alpine`; volume persistente |
| Cache / rate limiting | `infra/` | `kubernetes_deployment` + `kubernetes_service` | `redis` | Imagem `redis:7-alpine`; sem senha, sem persistência (`--save ""`) |
| E-mail local | `infra/` | `kubernetes_deployment` + `kubernetes_service` | `mailpit` | Imagem `axllent/mailpit:latest`; SMTP `1025`, web `8025` |

Os nomes dos Services (`postgres`, `redis`, `mailpit`) casam com `JDBC_HOST`, `REDIS_HOST` e
`MAIL_HOST` de `k8s/base/configmap.yaml` — nenhuma env var da aplicação precisa mudar.

## Pré-requisitos

1. **Terraform** ≥ 1.6 instalado.
2. Host Ubuntu/WSL2 com **snap** disponível e systemd ativo (`[boot] systemd=true` em `/etc/wsl.conf`).
3. `sudo` para snap/microk8s/usermod. Ver "Autenticação do sudo" abaixo.

### Autenticação do sudo

`infra/cluster/cluster.tf` roda `sudo snap install microk8s`, `sudo microk8s enable ...` etc.
dentro de um `local-exec` não-interativo — o Terraform não para no meio do apply para pedir senha.

- **Localmente**: rode `sudo -v` no mesmo terminal *antes* do Terraform (cache de 15 min no
  Ubuntu); os `sudo` do `local-exec` herdam a autenticação. Renove com `sudo -v` se o apply
  demorar mais que isso.

- **No runner self-hosted**: o usuário do runner precisa de **sudo sem senha** restrito a `snap`,
  `microk8s` e `usermod` — drop-in em `/etc/sudoers.d/`:

  ```
  <usuario-do-runner> ALL=(root) NOPASSWD: /usr/bin/snap, /snap/bin/microk8s, /usr/sbin/usermod
  ```

  O runner do GitHub Actions deve estar como serviço (`./svc.sh install && ./svc.sh start`) para
  subir junto com a distro.

## Variáveis

### `infra/cluster/`

| Variável | Default | Descrição |
| -------- | ------- | ---------- |
| `microk8s_channel` | `1.31/stable` | Canal do snap instalado quando o binário não existe |
| `microk8s_addons` | `["dns", "hostpath-storage", "metrics-server"]` | Addons garantidos a cada apply; `metrics-server` é obrigatório para o HPA (`k8s/base/hpa.yaml`) |
| `kubeconfig_path` | `~/.kube/config` | Onde escrever o kubeconfig; `infra/` lê do mesmo caminho |
| `apiserver_host` | `127.0.0.1` | Host que substitui o IP do apiserver no kubeconfig gerado |

### `infra/`

| Variável | Default | Descrição |
| -------- | ------- | ---------- |
| `kubeconfig_path` | `~/.kube/config` | Kubeconfig gerado por `infra/cluster/`; fonte do provider `kubernetes` |
| `kube_context` | `""` (context corrente) | Nome do context, se houver múltiplos clusters no kubeconfig |
| `namespace` | `tech-challenge` | Namespace onde os recursos são criados |
| `db_name` | `techbase` | Nome do banco PostgreSQL |
| `db_username` | `techdev` | Usuário do PostgreSQL |
| `db_password` | *(sem default — obrigatório)* | Senha do PostgreSQL; nunca commitar o valor real |
| `postgres_image` | `postgres:18.4-alpine` | Imagem do PostgreSQL |
| `postgres_storage_size` | `2Gi` | Tamanho do volume persistente do PostgreSQL |
| `redis_image` | `redis:7-alpine` | Imagem do Redis |
| `ghcr_username` | `johncgo` | Owner do pacote no GHCR; vira `docker-username` no Secret `ghcr-pull`. No CI vem de `TF_VAR_ghcr_username = github.repository_owner` |
| `ghcr_pull_token` | *(vazio)* | Token GitHub com escopo `read:packages`. Vazio = não cria o Secret `ghcr-pull`. No CI vem de `TF_VAR_ghcr_pull_token` (secret `GHCR_PULL_TOKEN`) |

Copie o exemplo e ajuste os valores reais (o `terraform.tfvars` é ignorado pelo git):

```bash
cp infra/terraform.tfvars.example infra/terraform.tfvars
# edite infra/terraform.tfvars e defina db_password com uma senha real
```

## Passos

```bash
# 1. bootstrap do cluster
cd infra/cluster
terraform init -backend-config="path=$HOME/.local/state/tech-challenge/cluster.tfstate"
terraform apply

# 2. recursos (namespace + Postgres/Redis/Mailpit)
cd ..
terraform init -backend-config="path=$HOME/.local/state/tech-challenge/terraform.tfstate"
terraform apply -var-file=terraform.tfvars
```

No CI o job `terraform-apply` faz exatamente essa sequência (ver `.github/workflows/ci-cd.yml`).

## Estado do Terraform (backend local)

Dois state files, ambos **fora do diretório de checkout**, passados no `terraform init` via
`-backend-config`:

- `$HOME/.local/state/tech-challenge/cluster.tfstate` — `infra/cluster/`
- `$HOME/.local/state/tech-challenge/terraform.tfstate` — `infra/`

O backend não pode viver dentro do cluster (backend `kubernetes`): o `init` roda antes de o
cluster existir. No runner self-hosted persistente os arquivos sobrevivem entre runs. Para
partilhar state entre máquinas, troque para um backend remoto (`s3`, `gcs`, `azurerm`).

Se os recursos `kubernetes_*` já existem no cluster mas não estão no state, importe-os antes do
primeiro `apply` de `infra/`:

```bash
cd infra
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

As chaves `dev-*` servem apenas para validação local. Não use em produção. Em seguida, aplique o
Kustomize (`k8s/overlays/local`) — ver `k8s/overlays/local/README.md`.

O Mailpit não exige autenticação SMTP; `mail-username` é só o endereço de remetente. Para ver as
mensagens:

```bash
kubectl -n tech-challenge port-forward service/mailpit 8025:8025
```

Acesse `http://localhost:8025`.

## Outputs

### `infra/cluster/`

| Output | Descrição |
| ------ | --------- |
| `kubeconfig_path` | Caminho do kubeconfig gerado |
| `apiserver` | Endereço do apiserver gravado no kubeconfig (`https://127.0.0.1:16443`) |

### `infra/`

| Output | Descrição |
| ------ | --------- |
| `namespace` | Namespace provisionado |
| `postgres_service` / `postgres_port` | Service e porta do PostgreSQL (`postgres` / `5432`) |
| `db_name` | Nome do banco (`techbase`) |
| `postgres_credentials_secret` | Nome do Secret com as credenciais do PostgreSQL |
| `redis_service` / `redis_port` | Service e porta do Redis (`redis` / `6379`) |
| `ghcr_pull_secret` | Nome do Secret de pull do GHCR, ou `null` quando não criado |

## Destruir os recursos

```bash
cd infra
terraform destroy -var-file=terraform.tfvars
```

Remove namespace, PostgreSQL, Redis e Mailpit. **Não desinstala o microk8s** — `infra/cluster/`
não tem provisioner de `destroy`, para não arriscar remover o cluster por engano. Para
desinstalar o microk8s (ex.: provar o provisionamento do zero), faça manualmente com
`sudo snap remove microk8s --purge` e rode `terraform -chdir=infra/cluster apply` de novo.
