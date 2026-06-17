# 🌿 PhenoAI Studio
## Documento de Arquitetura de Software
> *Software Architecture Document (SAD)*

**Versão:** 1.0 | **Data:** Junho 2026 | **Equipe:** Neon Space Engineering Team

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

| Serviço | Responsabilidade | Tecnologia | Porta |
|---|---|---|---|
| Auth Service | Autenticação, emissão e validação de JWT | Java 17 + Spring Boot 3.x | 8081 |
| User Service | CRUD de perfis, papéis e preferências | Java 17 + Spring Boot 3.x | 8082 |
| Credential Service | Armazenamento seguro de chaves de API (AES-256) | Java 17 + Spring Boot 3.x | 8083 |
| Model Catalog Service | Catálogo de modelos de IA e metadados | Java 17 + Spring Boot 3.x | 8084 |
| Upload Service | Validação e upload para S3, geração de metadata | Java 17 + Spring Boot 3.x | 8085 |
| Image Processing Service | Pré-processamento: resize, normalize, encode | Java 17 + Spring Boot 3.x | 8086 |
| AI Orchestration Service | Strategy Pattern: seleção de provedor/modelo, prompt engineering, chamadas de IA | Java 17 + Spring Boot 3.x | 8087 |
| Analysis Results Service | Persistência e consulta de resultados e histórico | Java 17 + Spring Boot 3.x | 8088 |
| Reporting Service | Geração de PDF/CSV a partir dos resultados | Java 17 + Spring Boot 3.x | 8089 |
| Frontend (Next.js) | Interface web responsiva e SSR | Next.js 14 + TypeScript | 3000 |
| API Gateway | Roteamento, rate limiting, autenticação centralizada | Spring Cloud Gateway / Nginx | 443 |

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
**Decisão:** Java 17 LTS com Spring Boot 3.x.
**Consequências:** Alta manutenibilidade, ecossistema Spring Security/Data/Cloud, curva de aprendizado moderada.

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

---

## 6. Infraestrutura e Deployment

### 6.1. Stack de Infraestrutura

| Camada | Tecnologia | Propósito |
|---|---|---|
| Containerização | Docker | Empacotamento de microsserviços |
| Orquestração | Kubernetes (K8s) | Gerenciamento, escala e self-healing |
| Service Mesh | Istio (opcional Fase 2) | mTLS, observabilidade, circuit breaking |
| Armazenamento de Objetos | Amazon S3 / MinIO | Imagens originais e processadas |
| Banco de Dados | PostgreSQL 15+ | Dados relacionais e JSONB |
| Cache | Redis 7+ | Cache de IA, sessões, rate limiting |
| Mensageria | Apache Kafka 3+ | Pipeline assíncrono de análise |
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
