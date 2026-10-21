# 🌿 PhenoAI Studio
## Guia de Implantação e Operações
> *Deployment & Operations Guide — Docker, K8s, CI/CD, Monitoring*

**Versão:** 1.1 | **Data:** Outubro 2026 | **Equipe:** Emely Atila Development

> **Status (v1.1):** o ambiente de desenvolvimento (seção 3) descreve o estado atual do repositório. As seções de CI/CD, Kubernetes, monitoramento e backup são o alvo de produção e ainda não estão implementadas. Não há workflows em `.github/workflows/` nem manifests em `k8s/`.

---


## 1. Pré-requisitos

| Ferramenta | Versão Mínima | Propósito |
|---|---|---|
| Docker | 24.x | Build e execução de containers |
| Docker Compose | 2.20+ | Orquestração local (desenvolvimento) |
| Kubernetes (kubectl) | 1.28+ | Orquestração em produção/staging |
| Helm | 3.12+ | Gerenciamento de charts K8s |
| Java JDK | 21 LTS (Temurin) | Build dos microsserviços backend (Gradle 8.8 via wrapper `backend/gradlew`) |
| Node.js | 20 LTS | Build do frontend Next.js |
| pnpm | 9.x | Gerenciador de pacotes do monorepo frontend (Turborepo) |
| PostgreSQL Client (psql) | 15+ | Acesso ao banco de dados |
| AWS CLI / MinIO Client | Latest | Gerenciamento do armazenamento de objetos |
| Helm Secrets / SOPS | Latest | Gerenciamento de secrets no pipeline CI/CD |

---

## 2. Ambientes

| Ambiente | Propósito | Branch Git | Deploy Automático |
|---|---|---|---|
| development | Desenvolvimento local com Docker Compose | feature/* ou develop | Não (manual) |
| staging | Homologação, testes E2E, validação de release | develop | Sim (push → deploy) |
| production | Produção com tráfego real de usuários | main (via tag) | Sim (tag → deploy) |

---

## 3. Ambiente de Desenvolvimento

### 3.1. Subindo o Stack com Docker Compose

```bash
# Clonar repositório
git clone https://github.com/org/phenoai-studio.git
cd phenoai-studio

# Copiar variáveis de ambiente de exemplo
cp .env.example .env
# Editar .env com chaves de API locais e configurações

# 1. Infraestrutura (PostgreSQL, Redis, Kafka, MinIO)
docker compose up -d
docker compose ps            # aguardar todos "healthy"

# 2. Microsserviços em container (profile "app": auth-service e user-service)
#    Os Dockerfiles copiam o JAR de build/libs, então é preciso gerar o JAR antes
cd backend && ./gradlew :auth-service:bootJar :user-service:bootJar && cd ..
docker compose --profile app up -d --build

# Alternativa: rodar um serviço direto do Gradle (lê o ../.env automaticamente)
cd backend && ./gradlew :auth-service:bootRun

# 3. (Opcional) Kafka UI
docker compose --profile tools up -d
```

As migrations Flyway rodam **automaticamente** quando cada serviço sobe. Não existe passo manual de migração.

| Acesso | URL |
|---|---|
| Frontend (`pnpm dev` em `frontend/`) | http://localhost:3000 |
| Auth Service / Swagger | http://localhost:8081 · http://localhost:8081/swagger-ui.html |
| User Service / Swagger | http://localhost:8082 · http://localhost:8082/swagger-ui.html |
| Kafka UI (profile `tools`) | http://localhost:8080 |
| MinIO Console | http://localhost:9001 |

> Ainda não existem API Gateway nem Grafana no `docker-compose.yml`. A porta 8080 está ocupada pelo Kafka UI. Quando o Gateway for adicionado, uma das duas portas precisa mudar.

### 3.2. Variáveis de Ambiente (.env)

Referência completa em `.env.example`. Variáveis em uso hoje:

| Variável | Descrição | Obrigatório |
|---|---|---|
| `POSTGRES_HOST` / `POSTGRES_PORT` / `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | Conexão com o PostgreSQL (o compose monta a `SPRING_DATASOURCE_URL` a partir delas) | Sim |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Conexão com o Redis | Sim |
| `KAFKA_HOST` / `KAFKA_PORT` / `KAFKA_UI_PORT` | Listener externo do Kafka e porta do Kafka UI | Sim |
| `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` / `MINIO_API_PORT` / `MINIO_CONSOLE_PORT` | Credenciais e portas do MinIO | Sim |
| `MINIO_BUCKET_IMAGES` / `MINIO_BUCKET_MODELS` | Buckets de imagens e modelos | Sim (Sprint 3+) |
| `JWT_SECRET` | Chave HMAC de assinatura dos JWT (gerar com `openssl rand -base64 64`). Precisa ser **a mesma** no Auth Service e no User Service | Sim |
| `JWT_ACCESS_TOKEN_EXPIRATION` / `JWT_REFRESH_TOKEN_EXPIRATION` | Validade dos tokens em segundos (padrão 3600 / 604800) | Não |
| `AES_SECRET_KEY` | Chave AES-256 do Credential Service (gerar com `openssl rand -base64 32`) | Sim (Credential Service) |
| `AUTH_SERVICE_PORT` … `ANALYSIS_SERVICE_PORT` | Portas dos microsserviços (8081–8086) | Não |
| `NEXT_PUBLIC_API_BASE_URL` / `NEXT_PUBLIC_APP_URL` / `NEXT_PUBLIC_DEFAULT_LOCALE` | Configuração do frontend | Sim (frontend) |

Variáveis planejadas, ainda não usadas pelo código:

| Variável | Descrição |
|---|---|
| `OPENAI_SYSTEM_API_KEY` / `ANTHROPIC_SYSTEM_API_KEY` | Chaves de sistema para o fallback de credencial (RF-35). As chaves dos usuários **não** ficam no `.env`. Elas são gerenciadas pelo Credential Service |
| `PROMETHEUS_PORT` / `GRAFANA_PORT` / `LOKI_PORT` / `JAEGER_UI_PORT` | Stack de observabilidade (comentadas no `.env.example`) |

---

## 4. Pipeline CI/CD — GitHub Actions

### 4.1. Workflow: Build e Teste (on: push → develop, feature/*)

1. Checkout do repositório e configuração do Java 21 (Temurin), por exemplo com `actions/setup-java` e `java-version: '21'`, `distribution: 'temurin'`
2. Build, testes unitários e de integração (Testcontainers) com verificação de cobertura: `./gradlew build -PincludeIntegration` em `backend/`. Sem a flag, os testes com `@Tag("integration")` são excluídos e o gate de cobertura do Jacoco (≥ 80%) não é aplicado. Não existe task `integrationTest` separada.
3. Frontend: `pnpm install --frozen-lockfile && pnpm lint && pnpm type-check && pnpm test && pnpm build` em `frontend/`
4. Análise SAST com SonarQube: verifica quality gates (coverage ≥ 80%, zero critical bugs)
5. Build da imagem Docker de cada microsserviço: `docker build -t registry/service:${GITHUB_SHA}`
6. Push das imagens para o registry: `ghcr.io/org/phenoai-{service}:{sha}`
7. Notificação de sucesso/falha via Slack/email para a equipe

### 4.2. Workflow: Deploy Staging (on: push → develop)

1. Trigger após sucesso do workflow de Build e Teste
2. Substituição de variáveis nos manifests Kubernetes com a nova tag de imagem
3. `kubectl apply -f k8s/staging/` com rollout progressivo (RollingUpdate, maxSurge: 1)
4. Execução de health checks: `kubectl rollout status deployment/{service} -n staging`
5. Execução de smoke tests automatizados contra o ambiente staging
6. Execução de testes E2E com Playwright contra staging
7. Deploy bloqueado automaticamente se smoke tests ou E2E falharem (rollback automático)

### 4.3. Workflow: Deploy Produção (on: tag v*.*.*)

1. Revisão manual obrigatória de pelo menos 2 engenheiros seniores antes do merge em main
2. Criação de tag semântica: `git tag v1.2.0 && git push origin v1.2.0`
3. Pipeline executa todos os testes novamente com as imagens da tag
4. Deploy com estratégia **Blue/Green** ou **Canary** (via Argo Rollouts) para zero downtime
5. Monitoramento pós-deploy: alertas ativos por 30 minutos; rollback automático se error rate > 1%

---

## 5. Kubernetes — Configuração de Produção

### 5.1. Namespaces

- `phenoai-production` — workloads de produção
- `phenoai-staging` — workloads de staging
- `phenoai-infra` — PostgreSQL, Redis, Kafka, Vault
- `monitoring` — Prometheus, Grafana, Loki, Jaeger

### 5.2. Recursos por Microsserviço (HPA)

| Serviço | Replicas Min | Replicas Max | CPU Request/Limit | Memory Request/Limit | HPA Trigger |
|---|---|---|---|---|---|
| Auth Service | 2 | 5 | 250m / 500m | 256Mi / 512Mi | CPU > 70% |
| Credential Service | 2 | 4 | 250m / 500m | 256Mi / 512Mi | CPU > 70% |
| AI Orchestration | 3 | 10 | 500m / 1000m | 512Mi / 1Gi | CPU > 60% |
| Image Processing | 2 | 8 | 500m / 1000m | 512Mi / 1Gi | CPU > 65% |
| Upload Service | 2 | 6 | 250m / 500m | 256Mi / 512Mi | CPU > 70% |
| Analysis Results | 2 | 5 | 250m / 500m | 256Mi / 512Mi | CPU > 70% |
| Reporting Service | 1 | 4 | 500m / 1000m | 512Mi / 1Gi | CPU > 70% |
| Frontend (Next.js) | 2 | 6 | 250m / 500m | 256Mi / 512Mi | CPU > 70% |

> User Service e Model Catalog Service ainda não têm dimensionamento definido nesta tabela.

---

## 6. Monitoramento e Observabilidade

### 6.1. Dashboards Grafana — Prioritários

- **Overview do Sistema:** latência geral, taxa de erros, throughput, disponibilidade por serviço
- **AI Orchestration:** requests por provedor, custo por análise, latência por modelo, cache hit rate
- **Credential Service:** tentativas de acesso, validações, falhas (alertas de segurança)
- **Pipeline de Análise:** mensagens Kafka pendentes, tempo de processamento end-to-end
- **Infraestrutura:** uso de CPU/memória dos pods, conexões de banco, uso do Redis

### 6.2. Alertas Críticos

| Alerta | Condição | Severidade | Ação |
|---|---|---|---|
| API Error Rate High | error_rate > 5% por 5min | CRITICAL | Notify on-call imediatamente |
| AI Cost Anomaly | custo/hora > 3x média | HIGH | Notify equipe IA + suspender análises |
| Credential Service Down | uptime < 100% por 1min | CRITICAL | PagerDuty + rollback automático |
| DB Connection Pool Exhausted | > 90% connections usadas | HIGH | Notify DBA + scale Pgpool |
| Kafka Lag > 1000 | consumer lag por 10min | MEDIUM | Notify DevOps + scale workers |
| Disk Usage > 80% | S3 ou PV > 80% | MEDIUM | Notify infra para expansão |

---

## 7. Backup e Recuperação de Desastres

- **PostgreSQL:** backup completo diário (`pg_dump`) para S3; WAL streaming para réplica de leitura; RTO < 4h; RPO < 1h
- **Redis:** RDB snapshot a cada 1h + AOF; dado de cache é perdável; somente sessões são críticas
- **S3/MinIO:** versioning habilitado; cross-region replication; lifecycle policy 90 dias
- **Kubernetes:** backup dos manifests no repositório Git (GitOps via ArgoCD)
- **Drill de recuperação:** simulação trimestral de recuperação completa em ambiente de DR
