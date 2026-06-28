# Tech Challenge — Postman Collection

Collection organizada por **domínio de negócio** e **fluxos de uso**, não por controllers.

## Importando

1. Abra o Postman
2. Clique em **Import**
3. Selecione `tech-challenge.postman_collection.json`
4. As variáveis de collection são criadas automaticamente

## Variáveis de collection

| Variável               | Descrição                                                     |
|------------------------|---------------------------------------------------------------|
| `baseUrl`              | URL base da API (padrão: `http://localhost:8080`)             |
| `accessToken`          | JWT do Atendente — populado pelo Setup                        |
| `accessTokenMechanic`  | JWT do Mecânico — populado pelo Setup                         |
| `userId`               | ID do usuário interno criado                                  |
| `customerUuid`         | UUID do cliente cadastrado                                    |
| `vehicleUuid`          | UUID do veículo cadastrado                                    |
| `serviceOrderUuid`     | UUID da Ordem de Serviço aberta                               |
| `mechanicalServiceUuid`| UUID do serviço mecânico do catálogo                         |
| `productUuid`          | UUID do produto no inventário                                 |
| `notificationUuid`     | UUID da primeira notificação da lista                         |

## Autorização

A collection usa **Bearer Token** (`{{accessToken}}`) herdado por todas as requisições.
Endpoints que precisam do token do **Mecânico** sobrescrevem com `{{accessTokenMechanic}}`.
Endpoints **públicos** (status de OS, aprovação de orçamento, entrega via OTP) desativam o auth.

---

## Estrutura das pastas

### Setup
Execute esta pasta **em ordem** antes de qualquer outra. Ela:
1. Cria usuário Atendente → salva `userId`
2. Faz login como Atendente → salva `accessToken`
3. Cria usuário Mecânico → salva `mechanicUserId`
4. Faz login como Mecânico → salva `accessTokenMechanic`
5. Cria serviço mecânico → salva `mechanicalServiceUuid`
6. Cria produto → salva `productUuid`
7. Cadastra cliente → salva `customerUuid`
8. Cadastra veículo → salva `vehicleUuid`

### Authentication
Login avulso. Útil quando o token expira durante os testes.

### Internal Users
CRUD de usuários internos (Atendentes e Mecânicos).
- Todos os campos são obrigatórios no create/update
- `login` e `email` são únicos e convertidos para lowercase

### Customer Management
Cadastro de clientes da oficina.
- CPF/CNPJ é **mascarado** nas respostas (ex: `***.456.789-**`)
- Use **Get Full Document (Unmasked)** para obter o documento completo (ATTENDANT only)
- Delete falha com `409` se houver Ordens de Serviço ativas

### Vehicle Management
Cadastro de veículos dos clientes.
- Placa aceita formato antigo (`ABC1234`) e Mercosul (`ABC1D23`)
- Delete falha com `409` se houver OS ativas

### Service Orders

Fluxo completo em 5 subfastas:

#### Opening
| Requisição                       | Notas                                              |
|----------------------------------|----------------------------------------------------|
| Open Service Order               | Salva `serviceOrderUuid`                           |
| List Service Orders              | Filtros opcionais: `status`, `customerUuid`, `from`, `to` |
| Get Service Order                | Retorna OS completa com Quote e linhas             |
| Get Service Order Status (Public)| Sem autenticação — para o cliente consultar online |

#### Diagnosis (Mecânico)
Executa na sequência: Start → Add Services/Products → Complete.
> Usa `{{accessTokenMechanic}}`.

| Requisição                          | Transição              |
|-------------------------------------|------------------------|
| Start Diagnosis                     | RECEIVED → IN_DIAGNOSIS |
| Add Mechanical Service              | Adiciona ao orçamento  |
| Remove Mechanical Service           | Remove do diagnóstico  |
| Add Product to Diagnosis            | Sem débito de estoque  |
| Complete Diagnosis                  | IN_DIAGNOSIS → AWAITING_APPROVAL + OTP enviado |

#### Quote Approval (Cliente via OTP)
Endpoints públicos — o cliente recebe o OTP por e-mail.
- **Approve**: avança para IN_PROGRESS
- **Reject**: cancela a OS
- **Resend OTP**: apenas ATTENDANT pode reenviar

#### Execution (Mecânico)
| Requisição              | Notas                                                        |
|-------------------------|--------------------------------------------------------------|
| Request Product         | Débita estoque. Item não-orçado cria adendo + novo OTP       |
| Return Product          | Requer reautenticação com senha. Só funciona se `returnable=true` |
| Complete Execution      | IN_PROGRESS → COMPLETED                                      |

#### Delivery
| Requisição                        | Notas                                          |
|-----------------------------------|------------------------------------------------|
| Accept Delivery (Attendant - JWT) | Usa o JWT herdado da collection                |
| Accept Delivery (Customer - OTP)  | Sem autenticação — body com token e documento  |
| Reject Delivery (Customer - OTP)  | COMPLETED → IN_PROGRESS (retrabalho)           |
| Close Dispute (Attendant)         | IN_PROGRESS → DISPUTED (encerramento forçado)  |

### Catalog
Serviços mecânicos oferecidos pela oficina.
- Create/Update/Delete: somente ATTENDANT
- List/Find/Avg Duration: ATTENDANT e MECHANIC
- Delete falha com `409` se o serviço estiver vinculado a OS ativa

### Inventory

#### Products
- `type`: `PART` ou `SUPPLY`
- `measurementUnit`: `UNIT`, `LITER`, `ML`, `KG`, `GRAM`, `METER`
- `returnable: false` impede devolução ao estoque após uso

#### Stock
| Requisição                | Notas                                                    |
|---------------------------|----------------------------------------------------------|
| Replenish Product Stock   | Incrementa `availableQuantity` + registro REPLENISHMENT  |
| Register Manual Adjustment| Auditoria apenas — não altera saldo                      |
| List Stock Movements      | Filtro opcional por `productId`                          |

### Notifications
Criadas automaticamente por eventos do sistema:
- `ORDER_APPROVED` / `QUOTE_REJECTED` / `DELIVERY_REJECTED` / `REWORK`
- `INSUFFICIENT_STOCK` / `QUOTE_EXPIRED` / `ORDER_DISPUTED`
- `OTP_DELIVERY_FAILED` / `OTP_LIMIT_EXCEEDED` / `ADDENDUM_PRODUCT_REJECTED`

---

## Fluxo completo de teste (Cenário 1)

```
Setup (pasta completa)
  → Customer Management / Register Customer
  → Vehicle Management / Register Vehicle
  → Service Orders / Opening / Open Service Order
  → Service Orders / Diagnosis / Start Diagnosis          (token mecânico)
  → Service Orders / Diagnosis / Add Mechanical Service   (token mecânico)
  → Service Orders / Diagnosis / Complete Diagnosis       (token mecânico)
  → Service Orders / Quote Approval / Approve Quote       (sem token — cliente)
  → Service Orders / Execution / Request Product          (token mecânico)
  → Service Orders / Execution / Complete Execution       (token mecânico)
  → Service Orders / Delivery / Accept Delivery (JWT)     (token atendente)
```

## Rodando localmente

```bash
docker-compose up -d
# Aguardar healthcheck do PostgreSQL (≈ 10s)
# Importar a collection e executar Setup
```

Swagger UI disponível em: `http://localhost:8080/swagger-ui.html`
