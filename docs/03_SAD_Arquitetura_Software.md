# 🌿 PhenoAI Studio
## Documento de Arquitetura de Software
> *Software Architecture Document (SAD)*

**Versão:** 1.1 | **Data:** Outubro 2026 | **Equipe:** Emely Atila Development

---


## 1. Visão Arquitetural

O PhenoAI Studio adota uma **arquitetura de microsserviços cloud-native**, projetada para escalabilidade horizontal, resiliência e manutenibilidade. A comunicação entre serviços é realizada via APIs REST síncronas para operações de baixa latência e via mensageria assíncrona (**Apache Kafka**) para processamento de imagens e chamadas de IA.

---

## 2. Princípios Arquiteturais

- **Domain-Driven Design (DDD):** cada microsserviço é alinhado a um bounded context
- **Clean Architecture:** separação em camadas (Controller → Service → Repository → Domain)
- **API-First Design:** todas as interfaces são definidas via OpenAPI/Swagger antes da implementação
- **Immutable Infrastructure:** uso de containers imutáveis gerenciados por Kubernetes
- **Security by Design:** segurança integrada em todas as camadas desde o design
- **Observability by Default:** cada serviço expõe métricas, logs e traces desde o início

---

## 3. Catálogo de Microsserviços

Todos os microsserviços backend usam **Java 21 + Spring Boot 3.2** (Gradle multi-project em `backend/`).

| Serviço | Módulo | Responsabilidade | Porta | Status |
|---|---|---|---|---|
| Auth Service | `auth-service` | Autenticação, emissão e validação de JWT | 8081 | Implementado |
| User Service | `user-service` | CRUD de perfis, papéis e preferências | 8082 | Implementado |
| Credential Service | `credential-service` | Armazenamento seguro de chaves de API (AES-256) | 8083 | Esqueleto (só a classe `Application`) |
| Model Catalog Service | `model-catalog-service` | Catálogo de modelos de IA e metadados | 8084 | Esqueleto |
| AI Orchestration Service | `ai-orchestration-service` | Strategy Pattern: seleção de provedor/modelo, prompt engineering, chamadas de IA | 8085 | Esqueleto |
| Analysis Results Service | `analysis-service` | Persistência e consulta de resultados e histórico | 8086 | Esqueleto |
| Upload Service | — | Validação e upload para S3, geração de metadata | a definir | Planejado (Sprint 3) |
| Image Processing Service | — | Pré-processamento: resize, normalize, encode | a definir | Planejado (Sprint 3) |
| Reporting Service | — | Geração de PDF/CSV a partir dos resultados | a definir | Planejado (Sprint 6) |
| Frontend (Next.js) | `frontend/apps/web` | Interface web responsiva e SSR | 3000 | Em andamento |
| API Gateway | — | Roteamento, rate limiting, autenticação centralizada | 443 | Planejado |

> As portas 8081–8086 são as definidas em `.env.example` e no `application.yml` de cada serviço. Os serviços ainda sem módulo receberão porta quando forem criados.

### 3.1. Biblioteca compartilhada

O módulo `shared` contém código comum aos microsserviços. Hoje ele traz o envelope de resposta `ApiResponse<T>` (`success`, `message`, `data`), usado pelo Auth Service.

---

## 4. Padrões de Design Utilizados

| Padrão | Aplicação no PhenoAI Studio |
|---|---|
| Strategy Pattern | AI Orchestration Service: seleção dinâmica de provedor/modelo de IA em runtime |
| Repository Pattern | Isolamento da camada de persistência em todos os microsserviços |
| Circuit Breaker | Resiliência nas chamadas externas a provedores de IA (Resilience4j) |
| Saga Pattern | Coordenação de transações distribuídas entre Upload → ImageProcessing → AI Orchestration |
| CQRS | Analysis Results Service: separação de queries (histórico) de commands (nova análise) |
| Outbox Pattern | Garantia de entrega de eventos no Kafka sem perda em caso de falha |
| API Gateway Pattern | Ponto único de entrada para todos os microsserviços |

---

## 5. Decisões Arquiteturais (ADRs)

### ADR-001: Java + Spring Boot como backend

**Contexto:** Necessidade de robustez, ecossistema maduro e suporte empresarial.
**Decisão:** Java 21 LTS com Spring Boot 3.2 (build com Gradle 8.8).
**Consequências:** Alta manutenibilidade, ecossistema Spring Security/Data/Cloud, curva de aprendizado moderada.
**Revisão (v1.1):** a versão inicial deste ADR previa Java 17. O backend foi configurado desde o primeiro commit com Java 21 (`sourceCompatibility`/`targetCompatibility` em `backend/build.gradle` e imagem `eclipse-temurin:21-jre-alpine` nos Dockerfiles). Java 21 também é LTS e é totalmente suportado pelo Spring Boot 3.2. Por isso o ADR foi atualizado para refletir o que está implementado.

### ADR-002: PostgreSQL com JSONB para resultados de IA

**Contexto:** Os resultados de IA são estruturados mas flexíveis (schema pode evoluir).
**Decisão:** PostgreSQL com campo JSONB para AnalysisResult.
**Consequências:** Flexibilidade sem migração de schema, capacidade de queries sobre o JSON, backup e replicação nativos.

### ADR-003: Kafka para comunicação assíncrona no pipeline de análise

**Contexto:** Processamento de imagens e chamadas de IA são operações pesadas que não devem bloquear a requisição HTTP.
**Decisão:** Apache Kafka para desacoplar Upload → Processing → AI → Results.
**Consequências:** Resiliência, reprocessamento de mensagens, escalabilidade independente por consumidor.

### ADR-004: Credential Service como serviço dedicado

**Contexto:** Chaves de API de IA são dados altamente sensíveis.
**Decisão:** Microsserviço dedicado com criptografia AES-256 em repouso, acesso restrito apenas ao AI Orchestration Service via mTLS.
**Consequências:** Isolamento de segurança, facilidade de auditoria, rotação de chaves sem impacto nos demais serviços.

### ADR-005: Banco compartilhado com um schema por serviço

**Contexto:** Na fase atual todos os microsserviços usam a mesma instância PostgreSQL (banco `phenoai`). Se todos os serviços usassem o schema `public`, as migrations `V1__` de serviços diferentes colidiriam na mesma tabela `flyway_schema_history`.
**Decisão:** Cada serviço possui seu próprio schema e roda o Flyway isolado nele (`spring.flyway.schemas` + `spring.flyway.default-schema`): `auth`, `users`, `credentials`, `catalog`, `analysis`. Os schemas são criados por `backend/db/init/01_create_schemas.sql`. Entidades JPA e SQL das migrations qualificam o schema explicitamente (`@Table(schema = ...)`, `CREATE TABLE auth.users`), porque `ddl-auto: validate` não herda o `default-schema` do Flyway.
**Consequências:** Isolamento por bounded context sem custo de várias instâncias de banco. Ainda falta criar um schema para o AI Orchestration Service. Migrar um serviço para banco próprio no futuro exige só trocar a URL de conexão.

### ADR-006: Logout por blacklist de access token no Redis

**Contexto:** JWT é stateless, então um token emitido continua válido até expirar.
**Decisão:** `POST /auth/logout` grava o access token no Redis (`auth:blacklist:<token>`) com TTL igual ao tempo restante de validade. O `JwtAuthFilter` do Auth Service rejeita tokens presentes na blacklist.
**Consequências:** Logout efetivo no Auth Service sem armazenar sessões. Limitações atuais: o refresh token não é invalidado, e os demais serviços (ex.: User Service) validam apenas assinatura e expiração, sem consultar a blacklist.

---

## 6. Infraestrutura e Deployment

### 6.1. Stack de Infraestrutura

| Camada | Tecnologia | Propósito |
|---|---|---|
| Containerização | Docker | Empacotamento de microsserviços |
| Orquestração | Kubernetes (K8s) | Gerenciamento, escala e self-healing |
| Service Mesh | Istio (opcional Fase 2) | mTLS, observabilidade, circuit breaking |
| Armazenamento de Objetos | Amazon S3 / MinIO | Imagens originais e processadas |
| Banco de Dados | PostgreSQL 15+ | Dados relacionais e JSONB (um schema por serviço — ADR-005) |
| Cache | Redis 7+ | Cache de IA, blacklist de tokens, rate limiting |
| Mensageria | Apache Kafka 3.6+ (KRaft, imagem `confluentinc/cp-kafka:7.6.1` no ambiente local) | Pipeline assíncrono de análise |
| Segredos | HashiCorp Vault / K8s Secrets | Credenciais de infraestrutura |
| CI/CD | GitHub Actions / GitLab CI | Build, test, deploy automático |
| Monitoramento | Prometheus + Grafana | Métricas e dashboards |
| Logging | Loki + Promtail + Grafana | Logs centralizados |
| Tracing | Jaeger (via OpenTelemetry) | Rastreamento distribuído |

---

## 7. Modelo de Segurança

### 7.1. Camadas de Segurança (Defense in Depth)

1. **Camada de Rede:** HTTPS/TLS 1.3 em todas as comunicações externas
2. **API Gateway:** autenticação JWT, rate limiting por usuário/IP, validação de origem
3. **Microsserviços:** autorização RBAC via Spring Security, validação de input
4. **Credential Service:** AES-256 em repouso, acesso via mTLS somente pelo AI Orchestration Service
5. **Banco de Dados:** criptografia em repouso, conexões TLS, usuários com privilégios mínimos
6. **S3/MinIO:** bucket policies, URLs assinadas com TTL, criptografia server-side
