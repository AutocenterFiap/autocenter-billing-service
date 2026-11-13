# Auto Center FIAP — Microsserviço de Orçamento e Pagamento (Billing)

Microsserviço responsável pelo ciclo completo de **orçamentos**, **cobranças** e **pagamentos** (com integração oficial **Mercado Pago Checkout Pro**) no ecossistema de microsserviços do **Tech Challenge — Fase 4**.

---

## 1. Arquitetura e Padrões

O serviço segue **Clean Architecture** (Domínio → Aplicação/UseCases → Adapters IN/OUT → Infraestrutura) com banco **MySQL 8** exclusivo e migrations gerenciadas via **Flyway** (`V1__criar_tabelas_billing.sql`).

### Escolha do Padrão Saga: Orquestração
O ecossistema adota o padrão **Saga Orquestrada**, onde o **`autocenter-os-service`** atua como orquestrador central e o `autocenter-billing-service` atua como participante autônomo:

- **Por que Orquestração (e não Coreografia)?**
  - Fluxo com múltiplas etapas síncronas/assíncronas e regras de retry de pagamento (até 3 tentativas), aprovação/reprovação de orçamento e compensação em cadeia (estorno + cancelamento).
  - Centralizar a máquina de estados da OS no `os-service` evita acoplamento ponto-a-ponto entre `billing` e `execucao` (os dois nunca se comunicam diretamente).
- **Resiliência e Idempotência**:
  - **Deduplicação por `eventId`**: tabela `eventos_processados` garante processamento idempotente mesmo com reentrega do Kafka.
  - **Retry + DLT**: consumers anotados com `@RetryableTopic` (backoff exponencial e envio para Dead Letter Topic em caso de falha persistente).
  - **Compensação Segura**:
    - `billing.estornar-pagamento` aciona o estorno real via `PaymentRefundClient` (com `X-Idempotency-Key`) e publica `pagamento.estornado` ou `pagamento.estorno-falhou`.
    - `os.cancelada` cancela orçamentos/pagamentos ativos, expira a preferência no Mercado Pago quando pendente e realiza estorno automático caso um pagamento tardio chegue para uma OS já cancelada.

---

## 2. Contrato Kafka

Todos os eventos usam `osId` como chave de partição e o envelope padrão (`EventoEnvelope<T>`):
`eventId`, `tipo`, `versao`, `osId`, `correlationId`, `ocorridoEm` (ISO-8601), `payload`.

### Eventos Consumidos

| Tópico | `tipo` | Ação no Billing |
|---|---|---|
| `billing.comandos` | `billing.gerar-orcamento` | Cria orçamento (`AGUARDANDO_APROVACAO`) e publica `orcamento.gerado` ou `orcamento.falhou` |
| `billing.comandos` | `billing.gerar-cobranca` | Cria pagamento (`PENDENTE`), gera preferência Checkout Pro e publica `pagamento.link-gerado` |
| `billing.comandos` | `billing.estornar-pagamento` | Estorna pagamento no Mercado Pago e publica `pagamento.estornado` ou `pagamento.estorno-falhou` |
| `os.eventos` | `os.cancelada` | Cancela orçamento/pagamento ativos da OS e expira preferência pendente no Mercado Pago |

### Eventos Publicados (`billing.eventos`)

| `tipo` | Quando é publicado |
|---|---|
| `orcamento.gerado` | Orçamento criado com sucesso |
| `orcamento.falhou` | Falha de validação ao gerar orçamento |
| `orcamento.aprovado` | Cliente aprova o orçamento |
| `orcamento.reprovado` | Cliente reprova o orçamento |
| `pagamento.link-gerado` | Preferência Checkout Pro gerada (`url`, `expiraEm`) |
| `pagamento.confirmado` | Pagamento aprovado (Webhook Mercado Pago ou endpoint de confirmação) |
| `pagamento.recusado` | Pagamento recusado/cancelado no Mercado Pago |
| `pagamento.estornado` | Estorno concluído com sucesso |
| `pagamento.estorno-falhou` | Falha ao estornar pagamento |

---

## 3. Endpoints REST

Documentação interativa disponível em **`/swagger-ui.html`** (`/api-docs`).

### Orçamentos (`/v1/orcamentos`)
- `GET /v1/orcamentos` — Lista paginada de orçamentos
- `GET /v1/orcamentos/{id}` — Busca orçamento por ID
- `GET /v1/orcamentos/os/{osId}` — Lista orçamentos da OS
- `PATCH /v1/orcamentos/{id}/aprovar` — Aprova orçamento e publica `orcamento.aprovado`
- `PATCH /v1/orcamentos/{id}/reprovar` — Reprova orçamento e publica `orcamento.reprovado`
- `POST /v1/orcamentos/{id}/enviar-cliente` — Registra envio ao cliente (`EMAIL`, `WHATSAPP`, `IMPRESSAO`)

### Pagamentos (`/v1/pagamentos`)
- `POST /v1/pagamentos` — Gera cobrança manualmente
- `GET /v1/pagamentos/{id}` — Busca pagamento por ID
- `GET /v1/pagamentos/os/{osId}` — Lista pagamentos da OS
- `POST /v1/pagamentos/webhook/mercadopago` — Webhook oficial do Mercado Pago (`type=payment`, `data.id`)
- `POST /v1/pagamentos/{id}/confirmar` — Confirma pagamento (auxiliar para demonstrações)
- `POST /v1/pagamentos/{id}/recusar` — Recusa pagamento (auxiliar para demonstrações)
- `POST /v1/pagamentos/{id}/estornar` — Estorna pagamento

### Observabilidade
- `/actuator/health` (`liveness` e `readiness`)
- `/actuator/prometheus`
- Logs estruturados em JSON (`logback-spring.xml`) com `correlationId` via MDC e agente Datadog no container.

---

## 4. Como Executar Localmente (e Integrar com os Outros Serviços)

### Pré-requisitos
- Java 21, Maven 3.9+, Docker (e `ngrok` caso queira testar o Webhook real do Mercado Pago Sandbox)

### Passo 1 — Configurar o `.env` (Mercado Pago Sandbox)
```bash
cp .env.example .env
```
Preencha no `.env`:
- **Com Mercado Pago Sandbox real (Checkout Pro + estorno real)**:
  ```env
  MERCADOPAGO_MODO=real
  MERCADOPAGO_ACCESS_TOKEN=APP_USR-...   # Access Token de TESTE da conta vendedora
  MERCADOPAGO_PUBLIC_KEY=APP_USR-...     # Public Key de TESTE
  ```
  > Para receber o **webhook automático** do Mercado Pago localmente, abra o túnel na porta `8099` (`make tunnel` ou `ngrok http 8099`). O serviço consulta `http://localhost:4040/api/tunnels` automaticamente ao criar cada cobrança e monta `<url-ngrok>/v1/pagamentos/webhook/mercadopago`. Se preferir uma URL fixa, defina `MERCADOPAGO_NOTIFICATION_URL` no `.env`.
  > No navegador, faça o pagamento usando uma **conta de comprador de teste** do Mercado Pago Sandbox (diferente da conta vendedora do token).
- **Sem conta do Mercado Pago (modo offline/simulado)**:
  ```env
  MERCADOPAGO_MODO=simulado
  ```
  Nesse modo o serviço gera links fictícios e a confirmação/recusa pode ser feita via `POST /v1/pagamentos/{id}/confirmar` ou `/recusar`.

### Passo 2 — Subir Infraestrutura (MySQL + Kafka) e Aplicação
```bash
make infra-up   # Sobe MySQL (porta 3307) e Kafka KRaft (porta 9092)
make run        # Sobe o billing-service na porta 8099 (profile dev lendo .env)
```

### Passo 3 — Rodando os 3 Microsserviços Juntos (Fluxo Completo da Saga)
Se você já tiver outro Kafka rodando na máquina (`localhost:9092`), suba apenas o MySQL do billing (`docker compose -f docker/docker-compose.yml up -d mysql`) ou aponte `KAFKA_BOOTSTRAP_SERVERS` para o broker único.

Portas recomendadas para evitar conflito local:
- **`autocenter-os-service`**: `8098` (padrão)
- **`autocenter-billing-service`**: `8099` (padrão)
- **`auto-center-execucao`**: sobrescreva com `SERVER_PORT=8097` (pois o padrão dele também é `8098`)

---

## 5. Testes, BDD e Cobertura

O projeto conta com:
- **Testes unitários** de domínio, casos de uso, controllers, consumers, publishers e adaptadores (34 testes).
- **Testes BDD com Cucumber** ([`billing.feature`](src/test/resources/features/billing.feature)) executando o fluxo completo ponta a ponta contra **MySQL 8** e **Kafka** reais via **Testcontainers**.
- **Gate de cobertura JaCoCo ≥ 80%** configurado no `pom.xml`.

```bash
mvn clean verify
```

### Evidência de Cobertura (`mvn clean verify`)
- **Testes executados**: `35` (`0` falhas, `0` erros, `0` ignorados — 34 unitários + 1 suite BDD Cucumber)
- **Cobertura medida (JaCoCo)**:
  - **Linhas (`LINE`)**: **94,23%** (`865 / 918`)
  - **Instruções (`INSTRUCTION`)**: **93,42%** (`3335 / 3570`)
  - **Métodos (`METHOD`)**: **97,96%** (`192 / 196`)
  - **Classes (`CLASS`)**: **98,15%** (`53 / 54`)
  - Relatório completo gerado em `target/site/jacoco/index.html` e publicado como artefato no CI (`.github/workflows/ci.yml`).

---

## 6. Docker, Kubernetes e CI/CD

- **Docker**: [`docker/Dockerfile`](docker/Dockerfile) (multi-stage Maven + JRE 21 + `dd-java-agent.jar`).
- **Kubernetes**: manifestos em [`k8s/`](k8s/README.md) (Namespace `auto-center-billing`, MySQL StatefulSet, Deployment com probes/Datadog, Service NodePort `30099`, HPA e Kustomize).
- **CI/CD**:
  - [`.github/workflows/ci.yml`](.github/workflows/ci.yml): build, testes unitários + BDD (Testcontainers), gate JaCoCo ≥ 80% e análise SonarQube.
  - [`.github/workflows/cd.yml`](.github/workflows/cd.yml): build/push da imagem no GHCR e deploy via `kubectl apply -k k8s/`.

