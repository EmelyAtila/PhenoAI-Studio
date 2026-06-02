# 🌿 PhenoAI Studio
## Guia de Implantação e Operações
> *Deployment & Operations Guide — Docker, K8s, CI/CD, Monitoring*

**Versão:** 1.0 | **Data:** Junho 2026 | **Equipe:** TechForge Senior Engineering Team

---


## 1. Pré-requisitos

| Ferramenta | Versão Mínima | Propósito |
|---|---|---|
| Docker | 24.x | Build e execução de containers |
| Docker Compose | 2.20+ | Orquestração local (desenvolvimento) |
| Kubernetes (kubectl) | 1.28+ | Orquestração em produção/staging |
| Helm | 3.12+ | Gerenciamento de charts K8s |
| Java JDK | 17 LTS (Temurin) | Build dos microsserviços backend |
| Node.js | 20 LTS | Build do frontend Next.js |
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

# Subir todos os serviços
docker-compose up -d

# Verificar status dos containers
docker-compose ps

# Aplicar migrations do banco
docker-compose exec auth-service ./gradlew flywayMigrate

# Acessos:
# Frontend:    http://localhost:3000
# API Gateway: http://localhost:8080
# Grafana:     http://localhost:3001 (admin/admin)
```

### 3.2. Variáveis de Ambiente Críticas (.env)

| Variável | Descrição | Obrigatório |
|---|---|---|
| `DATABASE_URL` | URL JDBC do PostgreSQL | Sim |
| `REDIS_URL` | URL do Redis (redis://host:6379) | Sim |
| `KAFKA_BOOTSTRAP_SERVERS` | Endereços dos brokers Kafka | Sim |
| `JWT_SECRET` | Chave secreta para assinatura de JWT (min 256 bits) | Sim |
| `CREDENTIAL_ENCRYPTION_KEY` | Chave AES-256 para criptografia das API keys | Sim |
| `S3_BUCKET_NAME` | Nome do bucket S3/MinIO para imagens | Sim |
| `S3_ENDPOINT` | Endpoint S3 (vazio para AWS, URL para MinIO) | Sim para MinIO |
| `AWS_ACCESS_KEY_ID` | Chave de acesso AWS/MinIO | Sim |
| `AWS_SECRET_ACCESS_KEY` | Chave secreta AWS/MinIO | Sim |
| `OPENAI_SYSTEM_API_KEY` | Chave OpenAI do sistema (fallback) | Recomendado |
| `ANTHROPIC_SYSTEM_API_KEY` | Chave Anthropic do sistema | Recomendado |

---

## 4. Pipeline CI/CD — GitHub Actions

### 4.1. Workflow: Build e Teste (on: push → develop, feature/*)

1. Checkout do repositório e configuração do Java 17 (Temurin)
2. Build e execução de testes unitários: `./gradlew test`
3. Execução de testes de integração com Testcontainers: `./gradlew integrationTest`
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
