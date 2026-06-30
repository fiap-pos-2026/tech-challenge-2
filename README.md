# The Java Garage — Sistema de Atendimento e Execução de Serviços

> **Tech Challenge — Fase 1 | Pós Tech FIAP 15SOAT**

Back-end MVP do sistema integrado de atendimento para oficina mecânica, com foco em gestão de ordens de serviço, clientes e peças, aplicando **Domain-Driven Design (DDD)** e boas práticas de **qualidade de software e segurança**.

---

## Contexto e Proposta

Uma oficina mecânica de médio porte utilizava anotações manuais e planilhas para gerenciar atendimentos, diagnósticos, execução e entrega de veículos. Isso gerava erros na priorização dos atendimentos, falhas no controle de peças, dificuldade de acompanhar status dos serviços, perda de histórico de clientes e ineficiência no fluxo de orçamentos e autorizações.

O sistema resolve esses problemas permitindo:

- Acompanhar o andamento da OS em tempo real via API (sem necessidade de login)
- Autorizar ou rejeitar orçamentos diretamente pelo cliente via link/OTP
- Controlar estoque de peças e insumos com auditoria por movimentação
- Gerenciar toda a operação interna com autenticação JWT por papel (Mecânico / Atendente)

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

### Linguagem Ubíqua

| Termo | Definição |
|---|---|
| **Ordem de Serviço (OS)** | Registro central do atendimento; agrega serviços, produtos, orçamento e status. |
| **Orçamento** | Composição de valores gerada automaticamente após o diagnóstico. |
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

### Arquitetura

Back-end monolítico com arquitetura em camadas (MVC clássico):

```
controller/   →  @RestController com DTOs de request/response
service/      →  Lógica de negócio (@Service)
repository/   →  Interfaces Spring Data JPA
domain/       →  Entidades JPA + enums de domínio
security/     →  Filtros JWT, autenticação, autorização por role
validation/   →  @ValidTaxId (CPF/CNPJ), @ValidLicensePlate
scheduler/    →  Expiração automática de orçamentos (7 dias)
```

---

## Pré-requisitos

- **Docker** e **Docker Compose** (recomendado)
- **JDK 25+** e **Gradle** (para execução local sem Docker)

---

## Como Executar

### Com Docker Compose (recomendado)

```bash
docker-compose up --build
```

Isso sobe a aplicação, o PostgreSQL, o Redis e o Mailpit (servidor SMTP local para OTPs em desenvolvimento). A aplicação estará disponível em `http://localhost:8080/core`.

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

---

## Variáveis de Ambiente

### JWT RSA

As chaves de desenvolvimento já estão em `core/src/main/resources/certs/`. **Não use essas chaves em produção.**

Para produção, forneça via variáveis de ambiente:

```
JWT_PUBLIC_KEY=<conteúdo da chave pública PEM>
JWT_PRIVATE_KEY=<conteúdo da chave privada PEM>
```

### Banco de Dados

```
DB_URL=jdbc:postgresql://localhost:5432/techbase
DB_USERNAME=techdev
DB_PASSWORD=techdevpw
```

### Redis

```
REDIS_HOST=localhost
REDIS_PORT=6379
```

Introduzido exclusivamente para suportar a funcionalidade de log-out com invalidação imediata de tokens JWT, 
implementada via blacklist. No docker-compose já está configurado apontando para o container `redis`.

### E-mail (OTP)

```
MAIL_HOST=smtp.seu-provedor.com
MAIL_PORT=587
MAIL_USERNAME=seu-usuario
MAIL_PASSWORD=sua-senha
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
```

Em desenvolvimento, o docker-compose sobe um **Mailpit** em `http://localhost:8025` que captura os e-mails localmente sem enviá-los.

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
| `POST` | `/api/service-orders` | ATTENDANT | Abrir OS com queixa do cliente |
| `GET` | `/api/service-orders` | ATTENDANT, MECHANIC | Listar OSs (filtros: status, customerUuid, from, to) |
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

### Fluxo de status (happy path)

```
RECEIVED → IN_DIAGNOSIS → AWAITING_APPROVAL → IN_PROGRESS → COMPLETED → DELIVERED
```

---

## Postman Collection

O arquivo `collections/tech-challenge.postman_collection.json` contém todas as requisições organizadas por domínio de negócio e fluxos de uso.

### Importando

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

Execute a pasta **Setup** completa primeiro — ela cria usuários, faz login e salva todos os UUIDs nas variáveis acima. Em seguida:

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

# Análise de dependências OWASP
./gradlew :core:dependencyCheckAnalyze
```

Os testes de integração sobem um PostgreSQL real via Testcontainers — o Docker precisa estar rodando.

**Suítes de teste:**

| Suíte | Testes | Cobertura |
|---|---|---|
| `ServiceOrderServiceTest` | 21 | Máquina de estados, orçamento, retrabalho, OTP, entrega |
| `StockServiceTest` | 7 | Débito, reposição, devolução, saldo insuficiente |
| `TaxIdValidatorTest` | 12 | Validação CPF e CNPJ (Módulo 11) |
| `LicensePlateValidatorTest` | 10 | Placas formato antigo e Mercosul |
| `SecurityIntegrationTest` | 8 | Autenticação JWT (banco real) |
| `ServiceOrderIntegrationTest` | 6 | Fluxo completo de OS (banco real) |

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
- **Custo e operação acessíveis** — para um sistema monolítico de oficina de porte médio, PostgreSQL gerenciado (RDS, Supabase, etc.) tem custo baixo e sem sobrecarga operacional.
