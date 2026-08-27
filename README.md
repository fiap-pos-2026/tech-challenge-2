# The Java Garage — Sistema de Atendimento e Execução de Serviços

> **Tech Challenge — Fase 1 e Fase 2 | Pós Tech FIAP 15SOAT**

Back-end do sistema integrado de atendimento para oficina mecânica, com foco em gestão de ordens de
serviço, clientes e peças, aplicando **Domain-Driven Design (DDD)** sobre uma **Clean Architecture**,
com qualidade de software, segurança e, na Fase 2, orquestração em Kubernetes, infraestrutura como
código e pipeline de CI/CD.

---

## Contexto e Proposta

Uma oficina mecânica de médio porte utilizava anotações manuais e planilhas para gerenciar atendimentos, diagnósticos, execução e entrega de veículos. Isso gerava erros na priorização dos atendimentos, falhas no controle de peças, dificuldade de acompanhar status dos serviços, perda de histórico de clientes e ineficiência no fluxo de orçamentos e autorizações.

O sistema resolve esses problemas permitindo:

- Acompanhar o andamento da OS em tempo real via API (sem necessidade de login)
- Receber e-mail a cada mudança de status da OS
- Autorizar ou rejeitar orçamentos diretamente pelo cliente via link/OTP
- Controlar estoque de peças e insumos com auditoria por movimentação
- Gerenciar toda a operação interna com autenticação JWT por papel (Mecânico / Atendente)

### Objetivos da Fase 2

A Fase 2 evolui o back-end da Fase 1 para atender qualidade, resiliência e escalabilidade sem trocar
o modelo arquitetural:

- Abertura de OS com serviços e peças opcionais já no ato do cadastro
- Listagem priorizada de OS (ordem de negócio + exclusão de Finalizada/Entregue por padrão)
- Notificação do cliente por e-mail a cada mudança de status da OS
- Deploy em **microk8s** local via **Kustomize**, com HPA, ConfigMaps e Secrets
- Infraestrutura de apoio (namespace, PostgreSQL, Redis e Mailpit) provisionada via
  **Terraform**
- **Pipeline CI/CD** (GitHub Actions, runner self-hosted) rodando build, testes, imagem e deploy

---

## Domínio (DDD)

### Bounded Contexts

```
┌──────────────────────────────────┐   ┌────────────────────────────────┐
│   Attendance (Core Domain)       │   │   Inventory (Supporting)       │
│                                  │   │                                │
│  Aggregate: ServiceOrder         │◄──│  Aggregate: Product            │
│  Aggregate: Customer             │   │  Entity:    StockMovement      │
│  Aggregate: Vehicle              │   └────────────────────────────────┘
│  Aggregate: MechanicalService    │
│  Entity:    Quote                │
└──────────────────────────────────┘
          ▲
          │ (Auth via JWT)
┌─────────┴────────────────────────┐
│   Identity (Generic Domain)      │
│   JWT Authentication             │
└──────────────────────────────────┘
```

- **Attendance** — contexto central; controla o ciclo de vida completo da Ordem de Serviço.
- **Inventory** — suporte; gerencia produtos, quantidades e movimentações de estoque.
- **Identity** — genérico; provê autenticação JWT sem lógica de negócio.

### Ciclo de Vida da Ordem de Serviço

```
RECEIVED → IN_DIAGNOSIS → AWAITING_APPROVAL → IN_PROGRESS → COMPLETED → DELIVERED
                                                   │
                                                   ├──► AWAITING_APPROVAL  (produto não orçado adicionado em execução)
                                                   ├──► CANCELLED          (orçamento expirado após 7 dias ou rejeitado)
                                                   └──► DISPUTED           (rejeição persistente na vistoria de entrega)
```

A cada transição de status persistida com sucesso (inclusive a abertura, que "transiciona" para
`RECEIVED`), o cliente recebe um e-mail com o identificador da OS e o novo status. Falha de SMTP é
registrada em log e **não** reverte a transição já persistida.

### Linguagem Ubíqua

| Termo | Definição |
|---|---|
| **Ordem de Serviço (OS)** | Registro central do atendimento; agrega serviços, produtos, orçamento e status. |
| **Orçamento** | Composição de valores gerada automaticamente após o diagnóstico (ou já na abertura, se houver itens). |
| **Aprovação de Orçamento** | Ação do Cliente autorizando a execução via OTP, sem necessidade de conta. |
| **Produto** | Termo guarda-chuva para Peças e Insumos controlados no estoque. |
| **Atendente** | Colaborador responsável pelo cadastro e acompanhamento administrativo da OS. |
| **Mecânico / Técnico** | Colaborador responsável pelo diagnóstico e execução dos serviços. |

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 25 |
| Framework | Spring Boot 4.1.0 |
| Build | Gradle Kotlin DSL |
| Banco de dados | PostgreSQL 18.4 |
| Cache / Rate limiting | Redis 7 |
| Migrations | Liquibase |
| Segurança | Spring Security + JWT RSA (OAuth2 Resource Server) |
| Documentação | Springdoc OpenAPI 3 (Swagger UI) |
| Testes de integração | Testcontainers |
| Cobertura | JaCoCo (gate ≥ 80% nos domínios críticos) |
| Orquestração (Fase 2) | Kubernetes (microk8s) via Kustomize |
| IaC (Fase 2) | Terraform |
| CI/CD (Fase 2) | GitHub Actions (runner self-hosted) |

### Arquitetura

Back-end monolítico em **Clean Architecture** — camadas concêntricas onde regras de negócio não
dependem de frameworks web ou de persistência:

```
controller/   →  Interface adapters: @RestController + DTOs de request/response
service/      →  Casos de uso / regras de aplicação (@Service)
domain/       →  Entidades de domínio + enums (núcleo, sem dependência de framework)
repository/   →  Interfaces Spring Data JPA (porta de saída para persistência)
security/     →  Filtros JWT, autenticação, autorização por role (infraestrutura)
validation/   →  @ValidTaxId (CPF/CNPJ), @ValidLicensePlate
scheduler/    →  Expiração automática de orçamentos (7 dias)
```

A Fase 2 manteve deliberadamente a Clean Architecture já adotada na Fase 1 — **não houve migração
para Hexagonal/ports-and-adapters**; os gaps de API e a fatia de plataforma (Kustomize, Terraform,
CI/CD) foram entregues sobre o layout existente.

### Arquitetura da infraestrutura

```mermaid
flowchart LR
  TF[Terraform] --> NS[Namespace tech-challenge]
  NS --> PG[(PostgreSQL)]
  NS --> Redis[(Redis)]
  NS --> Mailpit[Mailpit SMTP]
  K[Kustomize] --> Core[Deployment core]
  Core --> PG
  Core --> Redis
  Core --> Mailpit
  HPA[HPA CPU/memória] --> Core
  GA[GitHub Actions] --> Runner[Runner self-hosted WSL2]
  Runner --> TF
  Runner --> K
```

O Terraform provisiona as dependências de execução no microk8s existente. O Kustomize
provisiona a aplicação, o Service, o ConfigMap e o HPA. O Secret da aplicação é criado
fora do Git, porque contém a senha do banco, as credenciais SMTP e as chaves JWT.

---

## Pré-requisitos

- **Docker** e **Docker Compose** (recomendado para execução local)
- **JDK 25** e **Gradle** (para execução local sem Docker)
- **microk8s** no WSL2 com `kubectl` e Kustomize — para deploy em Kubernetes
- Addons microk8s `dns`, `storage`, `registry` e `metrics-server`
- **Terraform** ≥ 1.6 — para provisionar recursos via `/infra`
- Repositório GitHub com Actions habilitado — para validar o CI/CD

---

## Como Executar

### Com Docker Compose (recomendado para desenvolvimento)

```bash
docker-compose up --build
```

Isso sobe a aplicação, o PostgreSQL, o Redis e o Mailpit (servidor SMTP local que captura os e-mails
de OTP e de notificação de status em desenvolvimento — acesse `http://localhost:8025`). A aplicação
estará disponível em `http://localhost:8080/core`.

Para parar:

```bash
docker-compose down
```

### Executando localmente (sem Docker)

```bash
# 1. Suba o banco de dados
docker run -d \
  --name tech-pg \
  -e POSTGRES_USER=techdev \
  -e POSTGRES_PASSWORD=techdevpw \
  -e POSTGRES_DB=techbase \
  -p 5432:5432 \
  postgres:18.4-alpine

# 2. Execute a aplicação
./gradlew :core:bootRun
```

A aplicação sobe em `http://localhost:8080/core`.

Swagger UI disponível em: `http://localhost:8080/core/swagger-ui.html`

### Kubernetes local (microk8s + Kustomize)

Manifests em `/k8s`, organizados em `base` (Deployment, Service, ConfigMap, Secret de exemplo, HPA) e
overlay `local` (imagem do registry embutido do microk8s, NodePort, requests reduzidos para o nó
único do WSL2):

```bash
# 1. Verifique o cluster e habilite os addons necessários
microk8s status --wait-ready
microk8s enable dns storage registry metrics-server
microk8s kubectl get nodes

# 2. Provisione namespace, PostgreSQL, Redis e Mailpit
cd infra
cp terraform.tfvars.example terraform.tfvars
# Edite terraform.tfvars e informe db_password
terraform init
terraform plan -var-file=terraform.tfvars
terraform apply -var-file=terraform.tfvars
cd ..

# 3. Crie o Secret real da aplicação fora do Git
kubectl -n tech-challenge create secret generic tech-challenge-core-secret \
  --from-literal=jdbc-username=techdev \
  --from-literal=jdbc-password='<mesma senha do Terraform>' \
  --from-literal=mail-username=noreply@tech.local \
  --from-literal=mail-password='' \
  --from-file=jwt-public-key=core/src/main/resources/certs/dev-public.pem \
  --from-file=jwt-private-key=core/src/main/resources/certs/dev-private.pem

# 4. Publique a imagem no registry local
docker build -f core/Dockerfile \
  -t localhost:32000/tech-challenge-core:local .
docker push localhost:32000/tech-challenge-core:local

# 5. Revise e aplique o overlay Kustomize
kubectl kustomize k8s/overlays/local
kubectl apply -k k8s/overlays/local
```

A aplicação responde em `http://<ip-do-node>:30080/core`. Para descobrir o IP do
node, execute `hostname -I` no WSL2. Detalhes completos estão em
[`k8s/overlays/local/README.md`](k8s/overlays/local/README.md).

Para acompanhar o deploy:

```bash
kubectl -n tech-challenge get pods,services,hpa
kubectl -n tech-challenge rollout status deployment/core --timeout=180s
curl http://<ip-do-node>:30080/core/actuator/health
```

Para visualizar os e-mails capturados pelo Mailpit:

```bash
kubectl -n tech-challenge port-forward service/mailpit 8025:8025
```

Acesse `http://localhost:8025`.

### Terraform (infraestrutura de apoio)

O diretório `/infra` provisiona, via Terraform, o namespace, PostgreSQL, Redis e
Mailpit **sobre um microk8s já instalado**. O Terraform não instala o cluster:

```bash
cd infra
terraform init
cp terraform.tfvars.example terraform.tfvars   # defina db_password real
terraform plan -var-file=terraform.tfvars
terraform apply -var-file=terraform.tfvars
```

Passo a passo completo, variáveis, Secret da aplicação e outputs estão em
[`infra/README.md`](infra/README.md).

### CI/CD

O workflow [`.github/workflows/ci-cd.yml`](.github/workflows/ci-cd.yml) roda em um runner
**self-hosted** no WSL2 com acesso ao Docker, Terraform e microk8s. O runner precisa das
labels `self-hosted`, `wsl2` e `microk8s`.

#### Configurar o runner

1. No GitHub, abra **Settings → Actions → Runners → New self-hosted runner**.
2. Selecione **Linux** e **x64**.
3. No WSL2, execute os comandos de instalação exibidos pelo GitHub.
4. Configure o runner com as labels `wsl2,microk8s`.
5. Inicie-o com `./run.sh` e confirme que aparece como **Idle** no GitHub.

O usuário do runner precisa executar os seguintes comandos sem intervenção de senha:

```bash
docker version
terraform version
kubectl get nodes
```

Crie também o Secret do repositório **`TF_VAR_DB_PASSWORD`** em
**Settings → Secrets and variables → Actions**. O valor precisa ser igual à senha
usada pelo PostgreSQL.

#### Gatilhos do workflow

| Gatilho | Estágios executados |
|---|---|
| Push em `main` ou `feature/tech-challenge-2` | Build, testes, imagem, Terraform e deploy |
| Pull Request para `main` | Build e testes |
| `workflow_dispatch` | Build e testes |

O deploy só ocorre em `push`. A cadeia completa é:

```text
build-and-test → docker-image → terraform-apply → deploy
```

Qualquer falha em build, testes, imagem ou Terraform bloqueia os estágios seguintes.

---

## Variáveis de Ambiente

### JWT RSA

As chaves de desenvolvimento já estão em `core/src/main/resources/certs/`. **Não use essas chaves em produção.**

Para produção, forneça via variáveis de ambiente:

```
JWT_PUBLIC_KEY=<conteúdo ou caminho file: da chave pública PEM>
JWT_PRIVATE_KEY=<conteúdo ou caminho file: da chave privada PEM>
```

### Banco de Dados

```
JDBC_HOST=localhost
JDBC_PORT=5432
JDBC_SERVICENAME=techbase
JDBC_USERNAME=techdev
JDBC_PASSWORD=techdevpw
```

### Redis

```
REDIS_HOST=localhost
REDIS_PORT=6379
```

Introduzido exclusivamente para suportar a funcionalidade de log-out com invalidação imediata de tokens JWT, 
implementada via blacklist. No docker-compose e no Kustomize já está configurado apontando para o serviço `redis`.

### E-mail (OTP e notificação de status)

```
MAIL_HOST=smtp.seu-provedor.com
MAIL_PORT=587
MAIL_USERNAME=seu-usuario
MAIL_PASSWORD=sua-senha
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
```

Em desenvolvimento, o docker-compose sobe um **Mailpit** em `http://localhost:8025` que captura
localmente tanto os e-mails de OTP quanto os de notificação de mudança de status, sem enviá-los de
fato.

### Senha Inicial do Administrador

Na primeira inicialização, a aplicação detecta que o usuário `admin` ainda não trocou a senha e:

1. Se `ADMIN_INITIAL_PASSWORD` estiver definida como variável de ambiente, usa esse valor.
2. Caso contrário, **gera automaticamente uma senha aleatória de 16 caracteres**.
3. Em ambos os casos, **imprime a senha nos logs** com destaque:

```
=================================================================
  CREDENCIAL INICIAL DO ADMINISTRADOR
  Login : admin
  Senha : xK3@mQ7!rBv2#Yz9
  ALTERE ESTA SENHA IMEDIATAMENTE APÓS O PRIMEIRO LOGIN.
=================================================================
```

Após o primeiro login, o sistema bloqueia todas as operações e exige a troca da senha em `PUT /api/profile/password`.

---

## Endpoints da API

### Autenticação

| Método | Caminho | Auth | Descrição |
|---|---|---|---|
| `POST` | `/api/auth/login` | Público | Autenticação (retorna JWT) |
| `PUT` | `/api/profile/password` | JWT | Troca de senha obrigatória no primeiro login |

### Clientes

| Método | Caminho | Papel | Descrição |
|---|---|---|---|
| `POST` | `/api/customers` | ATTENDANT | Cadastrar cliente (CPF ou CNPJ) |
| `GET` | `/api/customers` | ATTENDANT, MECHANIC | Listar clientes |
| `GET` | `/api/customers/{uuid}` | ATTENDANT, MECHANIC | Buscar por ID |
| `GET` | `/api/customers/document/{taxId}` | ATTENDANT, MECHANIC | Buscar por CPF/CNPJ |
| `PUT` | `/api/customers/{uuid}` | ATTENDANT | Atualizar cliente |
| `DELETE` | `/api/customers/{uuid}` | ATTENDANT | Remover cliente |

### Veículos

| Método | Caminho | Papel | Descrição |
|---|---|---|---|
| `POST` | `/api/vehicles` | ATTENDANT | Cadastrar veículo |
| `GET` | `/api/vehicles` | ATTENDANT, MECHANIC | Listar veículos |
| `GET` | `/api/vehicles/{uuid}` | ATTENDANT, MECHANIC | Buscar por ID |
| `GET` | `/api/vehicles/license-plate/{plate}` | ATTENDANT, MECHANIC | Buscar por placa |
| `PUT` | `/api/vehicles/{uuid}` | ATTENDANT | Atualizar veículo |
| `DELETE` | `/api/vehicles/{uuid}` | ATTENDANT | Remover veículo |

### Catálogo de Serviços

| Método | Caminho | Papel | Descrição |
|---|---|---|---|
| `POST` | `/api/catalog/services` | ATTENDANT | Cadastrar serviço |
| `GET` | `/api/catalog/services` | ATTENDANT, MECHANIC | Listar serviços |
| `GET` | `/api/catalog/services/{uuid}` | ATTENDANT, MECHANIC | Buscar por ID |
| `PUT` | `/api/catalog/services/{uuid}` | ATTENDANT | Atualizar serviço |
| `DELETE` | `/api/catalog/services/{uuid}` | ATTENDANT | Remover serviço |
| `GET` | `/api/catalog/services/avg-duration` | ATTENDANT, MECHANIC | Tempo médio de execução por serviço |

### Produtos e Estoque

| Método | Caminho | Papel | Descrição |
|---|---|---|---|
| `POST` | `/api/catalog/products` | ATTENDANT | Cadastrar produto/insumo |
| `GET` | `/api/catalog/products` | ATTENDANT, MECHANIC | Listar produtos |
| `GET` | `/api/catalog/products/{uuid}` | ATTENDANT, MECHANIC | Buscar por ID |
| `PUT` | `/api/catalog/products/{uuid}` | ATTENDANT | Atualizar produto |
| `DELETE` | `/api/catalog/products/{uuid}` | ATTENDANT | Remover produto |
| `POST` | `/api/catalog/products/{uuid}/stock` | ATTENDANT | Ajuste manual de estoque |

### Ordens de Serviço

| Método | Caminho | Papel | Descrição |
|---|---|---|---|
| `POST` | `/api/service-orders` | ATTENDANT | Abrir OS com queixa do cliente e, opcionalmente, serviços/peças já na abertura (orçamento provisório) |
| `GET` | `/api/service-orders` | ATTENDANT, MECHANIC | Listar OSs priorizadas (`IN_PROGRESS` > `AWAITING_APPROVAL` > `IN_DIAGNOSIS` > `RECEIVED`, depois `createdAt` ASC); exclui `COMPLETED`/`DELIVERED` por padrão; filtros: `status`, `customerUuid`, `from`, `to` |
| `GET` | `/api/service-orders/{uuid}` | ATTENDANT, MECHANIC | Detalhar OS |
| `GET` | `/api/service-orders/{uuid}/status` | **Público** | Consultar status da OS (cliente) |
| `POST` | `/api/service-orders/{uuid}/diagnosis/start` | MECHANIC | Iniciar diagnóstico |
| `POST` | `/api/service-orders/{uuid}/diagnosis/services` | MECHANIC | Adicionar serviço ao diagnóstico |
| `DELETE` | `/api/service-orders/{uuid}/diagnosis/services/{itemId}` | MECHANIC | Remover serviço do diagnóstico |
| `POST` | `/api/service-orders/{uuid}/diagnosis/products` | MECHANIC | Adicionar produto ao diagnóstico |
| `POST` | `/api/service-orders/{uuid}/diagnosis/complete` | MECHANIC | Concluir diagnóstico (gera orçamento + envia OTP) |
| `POST` | `/api/service-orders/{uuid}/approval` | **Público (OTP)** | Aprovar ou rejeitar orçamento |
| `POST` | `/api/service-orders/{uuid}/otp/resend` | ATTENDANT | Reenviar OTP de aprovação |
| `POST` | `/api/service-orders/{uuid}/execution/products` | MECHANIC | Solicitar produto na execução (débito imediato) |
| `DELETE` | `/api/service-orders/{uuid}/execution/products/{productUuid}` | ATTENDANT | Devolver produto ao estoque |
| `POST` | `/api/service-orders/{uuid}/execution/complete` | MECHANIC | Concluir execução |
| `POST` | `/api/service-orders/{uuid}/delivery/accept` | ATTENDANT (JWT) ou **Público (OTP)** | Aceitar vistoria de entrega |
| `POST` | `/api/service-orders/{uuid}/delivery/reject` | **Público (OTP)** | Rejeitar vistoria de entrega |
| `POST` | `/api/service-orders/{uuid}/close-dispute` | ATTENDANT | Encerrar OS como DISPUTED |

Toda transição de status persistida com sucesso — incluindo a abertura — dispara e-mail ao cliente
com o identificador da OS e o novo status (falha de SMTP é logada e não reverte a transição).

### Fluxo de status (happy path)

```
RECEIVED → IN_DIAGNOSIS → AWAITING_APPROVAL → IN_PROGRESS → COMPLETED → DELIVERED
```

---

## Postman Collection e Swagger

- **Swagger UI**: `http://localhost:8080/core/swagger-ui.html` (com a aplicação rodando localmente)
- **Postman Collection**: [`collections/tech-challenge.postman_collection.json`](collections/tech-challenge.postman_collection.json) — todas as requisições organizadas por domínio de negócio e fluxos de uso, incluindo abertura de OS com itens opcionais e listagem priorizada

### Importando a collection

1. Abra o Postman
2. Clique em **Import**
3. Selecione `collections/tech-challenge.postman_collection.json`
4. As variáveis de collection são criadas automaticamente

### Variáveis de collection

| Variável | Descrição |
|---|---|
| `baseUrl` | URL base da API (padrão: `http://localhost:8080`) |
| `accessToken` | JWT do Atendente — populado pelo Setup |
| `accessTokenMechanic` | JWT do Mecânico — populado pelo Setup |
| `customerUuid` | UUID do cliente cadastrado |
| `vehicleUuid` | UUID do veículo cadastrado |
| `serviceOrderUuid` | UUID da Ordem de Serviço aberta |
| `mechanicalServiceUuid` | UUID do serviço mecânico do catálogo |
| `productUuid` | UUID do produto no inventário |

### Fluxo completo de teste (happy path)

```
Setup (pasta completa)
  → Service Orders / Opening / Open Service Order
  → Service Orders / Diagnosis / Start Diagnosis          (token mecânico)
  → Service Orders / Diagnosis / Add Mechanical Service   (token mecânico)
  → Service Orders / Diagnosis / Complete Diagnosis       (token mecânico)
  → Service Orders / Quote Approval / Approve Quote       (sem token — cliente via OTP)
  → Service Orders / Execution / Request Product          (token mecânico)
  → Service Orders / Execution / Complete Execution       (token mecânico)
  → Service Orders / Delivery / Accept Delivery (JWT)     (token atendente)
```

> Documentação completa das pastas e variáveis em [`collections/README.md`](collections/README.md).

---

## Executando os Testes

```bash
# Todos os testes (unitários + integração)
./gradlew :core:test

# Relatório de cobertura (HTML em core/build/reports/jacoco/test/html/index.html)
./gradlew :core:jacocoTestReport

# Verificação do gate de cobertura (≥ 80% nos domínios críticos)
./gradlew :core:jacocoTestCoverageVerification

# Build completo (usado como gate de CI)
./gradlew :core:build -x dependencyCheckAnalyze

# Análise de dependências OWASP
./gradlew :core:dependencyCheckAnalyze
```

Os testes de integração sobem um PostgreSQL real via Testcontainers — o Docker precisa estar rodando.

Qualquer teste falhando derruba `:core:test` e, na pipeline, bloqueia imagem e deploy. Para inspecionar
o relatório completo localmente sem interromper o build na primeira falha, exporte
`IGNORE_TEST_FAILURES=true` — a pipeline fixa essa variável em `false` e ignora o override.

---

## Segurança

### Autenticação e Autorização

- **JWT RSA (RS256)** — tokens assinados com par de chaves RSA; chaves nunca commitadas, fornecidas via variável de ambiente
- **RBAC** — papéis `MECHANIC` e `ATTENDANT`; `@PreAuthorize` em todos os endpoints protegidos
- **Endpoints públicos** — consulta de status, aprovação de orçamento e vistoria de entrega acessíveis sem JWT (cliente acessa via OTP)

### Validações de Domínio

- **CPF/CNPJ** — algoritmo Módulo 11 com dígitos verificadores (`@ValidTaxId`)
- **Placa de veículo** — formatos antigo (`AAA9999`) e Mercosul (`AAA9A99`) via regex (`@ValidLicensePlate`)
- **Senhas** — BCrypt; complexidade mínima: 8 caracteres, ao menos uma maiúscula, um número e um símbolo
- **Sem stack trace** — respostas de erro padronizadas via `@ControllerAdvice`; nunca expõem detalhes internos

### Segredos (Kubernetes / Terraform)

- Valores sensíveis (senha do banco, credenciais SMTP, chaves JWT) **nunca** entram no git — apenas
  exemplos/placeholders (`k8s/base/secret.example.yaml`, `infra/terraform.tfvars.example`)
- `k8s/.gitignore` e `infra/.gitignore` bloqueiam `secret.yaml`, `*.pem`, `*.tfstate` e `*.tfvars` reais
- Segredos reais são criados diretamente no cluster (`kubectl create secret`) ou via variável
  `TF_VAR_db_password` / secrets do runner self-hosted no CI

### OWASP A07:2021 — Identification and Authentication Failures

| Controle | Implementação |
|---|---|
| Sem credenciais padrão | `AdminCredentialInitializer` gera senha aleatória na primeira boot |
| Troca de senha obrigatória | Flag `force_change_password`; `PasswordChangeRequiredFilter` bloqueia 100% das rotas até a troca |
| Bloqueio por força bruta (login) | 5 tentativas → bloqueio de 15 min |
| Bloqueio por força bruta (troca de senha) | 5 tentativas de senha atual errada → bloqueio de 15 min |
| Reutilização de senha proibida | `changePassword` rejeita nova senha igual à atual |
| JWT com expiração | RS256 com expiração configurável via `jwt.expiry.duration` |
| Invalidação de token por hash | `TokenUtility.validate()` cruza claim `hash` com o banco |
| Conta inativa bloqueada | `JWTAuthorizationFilter` verifica flag `active` a cada request |
| Reautenticação para operações críticas | Devolução de produto exige confirmação de senha do Atendente |

### OWASP A09:2021 — Security Logging and Monitoring Failures

Todos os eventos críticos são registrados em `security_audit_log`:

| Evento | `AuditEventType` |
|---|---|
| Login bem-sucedido | `LOGIN_SUCCESS` |
| Login com credenciais inválidas | `LOGIN_FAILED` |
| Conta bloqueada | `OTP_INVALID_LIMIT` |
| Troca de senha bem-sucedida | `PASSWORD_CHANGED` |
| Reautenticação bem-sucedida | `REAUTHENTICATION_SUCCESS` |
| Reautenticação falha | `REAUTHENTICATION_FAILED` |
| Mudança de papel (role) | `USER_ROLE_CHANGED` |
| Encerramento de disputa | `DISPUTED_CLOSURE` |

---

## Relatórios de Segurança

| Relatório | Localização |
|---|---|
| Análise de vulnerabilidades da imagem Docker (Trivy) | [`reports/security/`](reports/security/README.md) |

---

## Justificativa da Escolha do Banco de Dados

O **PostgreSQL 18.4** foi escolhido pelas seguintes razões:

- **ACID completo** — operações de débito de estoque, aprovação de orçamento e transição de status da OS exigem transações com isolamento forte para evitar race conditions (ex.: `SELECT FOR UPDATE` no `StockService` para débito concorrente).
- **Modelo relacional alinhado ao domínio** — a OS centraliza relacionamentos entre `Customer`, `Vehicle`, `MechanicalService`, `Product` e `Quote`; o modelo relacional evita redundância e garante consistência referencial por FK.
- **Suporte maduro a JPA/Hibernate e Liquibase** — dialeto Hibernate estável; Liquibase tem suporte de primeira classe, permitindo migrações versionadas e reversíveis.
- **Compatibilidade com Testcontainers** — imagem oficial `postgres:18.4-alpine` usada nos testes de integração garante o mesmo banco em dev, teste e produção, eliminando discrepâncias de comportamento.
- **Custo e operação acessíveis** — para um sistema monolítico de oficina de porte médio, PostgreSQL gerenciado (RDS, Supabase, etc.) ou em cluster próprio (microk8s/Terraform) tem custo baixo e sem sobrecarga operacional.
