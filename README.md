# Tech Challenge Fase 1

Sistema de atendimento para oficina mecânica com controle de ordens de serviço, diagnóstico, orçamento, execução e entrega. Desenvolvido com Java 25 + Spring Boot 4.1.

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 25 |
| Framework | Spring Boot 4.1.0 |
| Build | Gradle Kotlin DSL |
| Banco de dados | PostgreSQL 16 |
| Migrations | Liquibase |
| Segurança | Spring Security + JWT RSA (OAuth2 Resource Server) |
| Documentação | Springdoc OpenAPI 3 (Swagger UI) |
| Testes de integração | Testcontainers |

---

## Pré-requisitos

- JDK 25+
- Docker (para o banco de dados e testes de integração)
- PostgreSQL 16 (via Docker é recomendado)

---

## Configuração

### 1. Banco de dados

```bash
docker run -d \
  --name tech-pg \
  -e POSTGRES_USER=techdev \
  -e POSTGRES_PASSWORD=techdevpw \
  -e POSTGRES_DB=techbase \
  -p 5432:5432 \
  postgres:16-alpine
```

### 2. Chaves JWT RSA (desenvolvimento)

As chaves de desenvolvimento já estão em `core/src/main/resources/certs/`. **Não use essas chaves em produção.**

Para produção, forneça via variáveis de ambiente:

```
JWT_PUBLIC_KEY=<caminho ou conteúdo da chave pública>
JWT_PRIVATE_KEY=<caminho ou conteúdo da chave privada>
```

### 3. E-mail (OTP)

O envio de OTP por e-mail requer as seguintes variáveis de ambiente:

```
MAIL_HOST=smtp.seu-provedor.com
MAIL_PORT=587
MAIL_USERNAME=seu-usuario
MAIL_PASSWORD=sua-senha
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
```

---

## Executando a aplicação

```bash
./gradlew :core:bootRun
```

A aplicação sobe em `http://localhost:8080/core`.

Swagger UI disponível em: `http://localhost:8080/core/swagger-ui.html`

---

## Executando os testes

```bash
# Testes unitários e de integração
./gradlew :core:test

# Verificação de dependências (OWASP)
./gradlew :core:dependencyCheckAnalyze
```

Os testes de integração sobem um PostgreSQL via Testcontainers — certifique-se que o Docker está rodando.

---

## Principais endpoints

| Método | Caminho | Papel | Descrição |
|---|---|---|---|
| POST | `/api/auth/login` | - | Autenticação (retorna JWT) |
| POST | `/api/customers` | ATTENDANT | Cadastrar cliente |
| GET | `/api/customers/{uuid}` | ATTENDANT, MECHANIC | Buscar cliente |
| POST | `/api/vehicles` | ATTENDANT | Cadastrar veículo |
| POST | `/api/service-orders` | ATTENDANT | Abrir OS |
| GET | `/api/service-orders` | ATTENDANT, MECHANIC | Listar OSs |
| GET | `/api/service-orders/{uuid}` | ATTENDANT, MECHANIC | Buscar OS |
| POST | `/api/service-orders/{uuid}/diagnosis/start` | MECHANIC | Iniciar diagnóstico |
| POST | `/api/service-orders/{uuid}/diagnosis/services` | MECHANIC | Adicionar serviço |
| POST | `/api/service-orders/{uuid}/diagnosis/products` | MECHANIC | Adicionar produto |
| POST | `/api/service-orders/{uuid}/diagnosis/complete` | MECHANIC | Concluir diagnóstico (envia OTP) |
| POST | `/api/service-orders/{uuid}/approval` | Público (OTP) | Aprovar ou rejeitar orçamento |
| POST | `/api/service-orders/{uuid}/otp/resend` | ATTENDANT | Reenviar OTP |
| POST | `/api/service-orders/{uuid}/execution/products` | MECHANIC | Solicitar produto (débito imediato) |
| DELETE | `/api/service-orders/{uuid}/execution/products/{productUuid}` | ATTENDANT | Devolver produto |
| POST | `/api/service-orders/{uuid}/execution/complete` | MECHANIC | Concluir execução |
| POST | `/api/service-orders/{uuid}/delivery/accept` | ATTENDANT (JWT) ou Público (OTP) | Aceitar entrega |
| POST | `/api/service-orders/{uuid}/delivery/reject` | Público (OTP) | Rejeitar entrega |
| POST | `/api/service-orders/{uuid}/close-dispute` | ATTENDANT | Encerrar como DISPUTED |

---

## Fluxo principal (happy path)

```
RECEIVED → IN_DIAGNOSIS → AWAITING_APPROVAL → IN_PROGRESS → COMPLETED → DELIVERED
```

---

## Justificativa da escolha do banco de dados

O PostgreSQL 16 foi escolhido pelas seguintes razões:

- **ACID completo**: as operações de débito de estoque, aprovação de orçamento e transição de estado da OS exigem transações com isolamento forte para evitar race conditions (ex.: débito concorrente de produtos — `SELECT FOR UPDATE` no `StockService`).
- **JSONB / tipos avançados**: embora não utilizados neste MVP, o PostgreSQL permite evoluir a modelagem de atributos de produto sem alterar o esquema relacional.
- **Suporte maduro ao JPA/Hibernate e Liquibase**: Flyway e Liquibase têm suporte de primeira classe para PostgreSQL; o dialeto Hibernate é estável e bem testado.
- **Compatibilidade com Testcontainers**: imagem oficial `postgres:18.4-alpine` usada nos testes de integração — o mesmo banco em dev, teste e produção elimina discrepâncias de comportamento.
- **Custo zero e operação simples**: para um sistema monolítico de oficina mecânica de porte pequeno/médio, PostgreSQL gerenciado (RDS, Supabase, etc.) tem custo acessível e sem sobrecarga operacional.

