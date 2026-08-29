# Infraestrutura (`/infra`) — Terraform: cluster + recursos de apoio

Provisiona, via Terraform, o cluster Kubernetes local (**microk8s**, WSL2/Ubuntu) e os recursos de
apoio necessários para a aplicação `core` — namespace, PostgreSQL, Redis e Mailpit. Um único
`terraform apply` cobre as duas pontas do requisito "provisionamento do cluster Kubernetes (local
ou cloud)": o cluster em si (`cluster.tf`) e o banco de dados (`main.tf`).

## Recursos criados

| Recurso | Tipo Terraform | Nome no cluster | Observação |
| ------- | -------------- | ---------------- | ---------- |
| Cluster Kubernetes | `null_resource` + `local-exec` (`cluster.tf`) | microk8s (serviço snap no host) | Instala o snap se ausente, habilita addons `dns`/`storage`/`metrics-server` e gera o kubeconfig; idempotente — pula o que já está pronto. Desligável via `manage_microk8s = false` (ver abaixo) |
| Namespace da aplicação | `kubernetes_namespace` | `tech-challenge` (var `namespace`) | Mesmo namespace usado pelo Kustomize em `/k8s` |
| Credenciais do PostgreSQL | `kubernetes_secret` | `postgres-credentials` | `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` |
| Banco de dados | `kubernetes_stateful_set` + `kubernetes_service` | `postgres` | Imagem `postgres:18.4-alpine` (mesma do `docker-compose.yml`); volume persistente |
| Cache / rate limiting | `kubernetes_deployment` + `kubernetes_service` | `redis` | Imagem `redis:7-alpine` (mesma do `docker-compose.yml`); sem senha, sem persistência (`--save ""`) |
| E-mail local | `kubernetes_deployment` + `kubernetes_service` | `mailpit` | Imagem `axllent/mailpit:latest`; SMTP na porta `1025` e interface web na `8025` |

Os nomes dos Services (`postgres`, `redis`, `mailpit`) foram escolhidos para casar exatamente com
`JDBC_HOST`, `REDIS_HOST` e `MAIL_HOST` já definidos em `k8s/base/configmap.yaml` — nenhuma variável
de ambiente da aplicação precisa mudar para consumir esta infra.

### Por que microk8s via `local-exec` em vez de um provider de cluster

microk8s roda como serviço no próprio host (WSL2/Ubuntu) — não é algo que o provider `kubernetes`
cria, pois esse provider já pressupõe um cluster respondendo. Por isso `cluster.tf` usa um
`null_resource` com `local-exec`: instala o snap do microk8s quando ausente, aguarda o cluster
ficar pronto, habilita os addons e escreve o kubeconfig que os providers `kubernetes`/`helm`
(`providers.tf`) passam a usar. O script é idempotente — reexecuta sem efeito colateral se o
cluster já existir — para caber tanto no primeiro `apply` quanto nas execuções seguintes do CI.

### Cobrindo a opção "cloud" do mesmo requisito

Para apontar para um cluster gerenciado (EKS/GKE/AKS) ou qualquer cluster já provisionado por
outro meio, defina `manage_microk8s = false`: o Terraform pula a instalação do microk8s e só se
conecta ao cluster via `kubeconfig_path`/`kube_context`, mantendo namespace, banco e demais
recursos deste módulo exatamente iguais.

## Pré-requisitos

1. **Terraform** ≥ 1.6 instalado.
2. Host Ubuntu/WSL2 com **snap** disponível.
3. `sudo` funcionando normalmente (com senha) — este módulo **não exige e não configura
   `sudo` sem senha**. Ver "Autenticação do sudo" abaixo para como isso funciona na prática,
   tanto localmente quanto no CI.
4. Se `manage_microk8s = false`: um cluster Kubernetes já existente (local ou cloud) e seu
   **kubeconfig** disponível, apontado pela variável `kubeconfig_path`.

### Autenticação do sudo

`cluster.tf` roda `sudo snap install microk8s`, `sudo microk8s enable ...` etc. dentro de um
`local-exec`, que não é interativo — não há como o Terraform "parar" no meio do apply e pedir sua
senha. Por isso este módulo usa dois fluxos diferentes, sem NOPASSWD em sudoers em nenhum dos
dois:

- **Localmente (você, no WSL, com `manage_microk8s = true`)**: autentique o `sudo` *antes* de
  rodar o Terraform, no mesmo terminal:

  ```bash
  sudo -v
  ```

  Isso pede sua senha uma única vez e guarda a autorização no cache do sudo (15 min por padrão
  no Ubuntu). Enquanto esse cache estiver válido, os comandos `sudo` dentro do `local-exec`
  herdam a autenticação sem pedir senha de novo. Se a instalação do microk8s demorar mais que
  isso, rode `sudo -v` de novo em outro terminal para renovar o cache sem interromper o apply.

- **No runner self-hosted do CI (`manage_microk8s = false`)**: o CI nunca roda comandos que
  exigem sudo. O microk8s do runner é instalado e habilitado **manualmente, uma única vez**,
  por quem configura o runner (com `sudo` interativo normal — os mesmos comandos que
  `cluster.tf` automatiza, rodados à mão):

  ```bash
  sudo snap install microk8s --classic --channel=1.31/stable
  sudo usermod -aG microk8s "$USER"
  # relogue o shell para o grupo `microk8s` valer, depois:
  microk8s status --wait-ready
  microk8s enable dns storage metrics-server
  mkdir -p ~/.kube
  microk8s config > ~/.kube/config
  ```

  Depois disso, o Terraform do CI só se conecta ao cluster já pronto via `kubeconfig_path` —
  ver a variável `TF_VAR_manage_microk8s: "false"` em `.github/workflows/ci-cd.yml`.

## Variáveis

| Variável | Default | Descrição |
| -------- | ------- | ---------- |
| `manage_microk8s` | `true` | Se `true`, o Terraform instala/habilita o microk8s (`cluster.tf`). Se `false`, só se conecta a um cluster já existente |
| `microk8s_channel` | `1.31/stable` | Canal do snap do microk8s a instalar |
| `microk8s_addons` | `["dns", "storage", "metrics-server"]` | Addons habilitados; `metrics-server` é obrigatório para o HPA (`k8s/base/hpa.yaml`) |
| `kubeconfig_path` | `~/.kube/config` | Caminho do kubeconfig — destino de `microk8s config` quando `manage_microk8s = true`, ou caminho de um kubeconfig existente quando `false` |
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

## Estado compartilhado pelo CI

O estado do Terraform fica no backend Kubernetes, armazenado em um Secret no namespace
`default`. Assim, o runner self-hosted, o CI e as execuções locais usam o mesmo estado. O
arquivo `terraform.tfstate` local continua ignorado pelo Git.

Na primeira configuração, migre o estado local existente para o backend. Execute este comando
uma única vez no ambiente que contém o `infra/terraform.tfstate` atual:

```bash
cd infra
terraform init -migrate-state -force-copy
terraform plan -var-file=terraform.tfvars
```

Confirme que o plano não tenta adicionar os recursos já existentes antes de executar o `apply`.
O backend cria um Secret com nome no formato `tfstate-default-tech-challenge`.

Se o estado local não estiver disponível, importe os recursos existentes antes do primeiro
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
| `cluster_managed_by_terraform` | `true` se o microk8s foi instalado/habilitado por este módulo |
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
`null_resource.microk8s` só instala/habilita (é deliberadamente sem efeito no `destroy`, para não
arriscar remover o cluster e seu estado por engano). Para desinstalar o microk8s, faça manualmente
com `sudo snap remove microk8s`.
