# PhenoAI Studio

Plataforma de análise fenotípica de plantas potencializada por IA. Permite que pesquisadores enviem imagens, escolham modelos de IA e obtenham análises fenotípicas detalhadas.

## Arquitetura

```
phenoai-studio/
├── backend/                    # Gradle multi-project (Java 21 + Spring Boot 3.x)
│   ├── shared/                 # Biblioteca compartilhada entre microsserviços
│   ├── auth-service/           # Autenticação JWT (porta 8081)
│   ├── user-service/           # Perfis de usuário (porta 8082)
│   ├── credential-service/     # Chaves de API criptografadas (porta 8083)
│   ├── model-catalog-service/  # Catálogo de modelos de IA (porta 8084)
│   ├── ai-orchestration-service/ # Orquestração de chamadas IA (porta 8085)
│   ├── analysis-service/       # Resultados e histórico de análises (porta 8086)
│   └── db/init/                # Criação dos schemas PostgreSQL (um por serviço)
├── frontend/                   # Turborepo (Next.js 14 + TypeScript)
│   └── apps/
│       └── web/                # Aplicação principal (porta 3000)
├── docker-compose.yml          # Infraestrutura local
└── docs/                       # Documentação técnica completa
```

**Stack:** Java 21 · Spring Boot 3.2 · Gradle 8.8 · Next.js 14 · PostgreSQL 15 · Redis 7 · Kafka 3.6 (KRaft, Confluent 7.6.1) · MinIO

**Status:** Auth Service e User Service estão implementados. Os demais microsserviços ainda são esqueletos, e o frontend tem setup e i18n prontos.

---

## Pré-requisitos

| Ferramenta | Versão mínima |
|---|---|
| Docker Desktop | 24.x |
| Docker Compose | v2 (incluído no Docker Desktop) |
| Java JDK | 21 |
| Node.js | 20 LTS |
| pnpm | 9.x |

---

## Setup local — infraestrutura

### 1. Configurar variáveis de ambiente

```bash
cp .env.example .env
```

Edite `.env` e configure ao menos:
- `POSTGRES_PASSWORD` — senha do banco de dados
- `REDIS_PASSWORD` — senha do Redis
- `JWT_SECRET` — gere com `openssl rand -base64 64`
- `AES_SECRET_KEY` — gere com `openssl rand -base64 32`

### 2. Subir os serviços de infraestrutura

```bash
# Infraestrutura base (PostgreSQL, Redis, Kafka, MinIO)
docker-compose up -d

# Verificar status e health checks
docker-compose ps
```

Todos os serviços devem mostrar status `healthy` em até 60 segundos.

### 3. Verificar os serviços

| Serviço | URL | Credenciais |
|---|---|---|
| PostgreSQL | `localhost:5432` | `phenoai / phenoai_secret` (padrão) |
| Redis | `localhost:6379` | senha: `redis_secret` (padrão) |
| Kafka | `localhost:9092` | — |
| MinIO Console | http://localhost:9001 | `minioadmin / minio_secret_2024` (padrão) |

```bash
# Testar PostgreSQL
docker exec phenoai-postgres pg_isready -U phenoai -d phenoai

# Testar Redis
docker exec phenoai-redis redis-cli -a redis_secret ping

# Testar Kafka
docker exec phenoai-kafka kafka-topics --bootstrap-server localhost:9092 --list

# Testar MinIO
curl http://localhost:9000/minio/health/live
```

### 4. Subir com UI do Kafka (opcional)

```bash
docker-compose --profile tools up -d
# Kafka UI disponível em: http://localhost:8080
```

---

## Setup local — backend

```bash
cd backend

# Verificar configuração do Gradle
./gradlew projects

# Build de todos os serviços
./gradlew build

# Rodar Auth Service individualmente (lê o ../.env automaticamente)
./gradlew :auth-service:bootRun

# Rodar testes unitários
./gradlew test

# Rodar também os testes de integração (Testcontainers, exige Docker) + gate de cobertura 80%
./gradlew build -PincludeIntegration
```

As migrations Flyway rodam automaticamente na subida de cada serviço, cada uma no schema do seu serviço.

### Rodar os serviços em container

Os serviços backend ficam no profile `app` do Compose. Os Dockerfiles copiam o JAR já compilado, então é preciso gerar o JAR antes:

```bash
cd backend && ./gradlew :auth-service:bootJar :user-service:bootJar && cd ..
docker-compose --profile app up -d --build
```

| Serviço | URL | Swagger |
|---|---|---|
| Auth Service | http://localhost:8081 | http://localhost:8081/swagger-ui.html |
| User Service | http://localhost:8082 | http://localhost:8082/swagger-ui.html |

---

## Setup local — frontend

```bash
cd frontend

# Instalar dependências
pnpm install

# Desenvolvimento
pnpm dev

# Build de produção
pnpm build
```

A aplicação estará disponível em http://localhost:3000.

---

## Comandos úteis

```bash
# Parar todos os containers
docker-compose down

# Parar e remover volumes (reset completo do banco de dados)
docker-compose down -v

# Ver logs de um serviço específico
docker-compose logs -f postgres
docker-compose logs -f kafka

# Reiniciar um serviço
docker-compose restart redis
```

---

## Estrutura de branches

| Branch | Propósito |
|---|---|
| `main` | Código em produção |
| `develop` | Integração contínua |
| `feature/*` | Novas funcionalidades |
| `fix/*` | Correções de bugs |

---

## Documentação técnica

A documentação completa está em [`docs/`](./docs/):

- [`01_Vision_Scope.md`](./docs/01_Vision_Scope.md) — Visão e escopo do produto
- [`02_SRS_Especificacao_Requisitos.md`](./docs/02_SRS_Especificacao_Requisitos.md) — Requisitos de software
- [`03_SAD_Arquitetura_Software.md`](./docs/03_SAD_Arquitetura_Software.md) — Arquitetura do sistema
- [`04_API_Design_Specification.md`](./docs/04_API_Design_Specification.md) — Especificação das APIs
- [`05_Frontend_Documentation.md`](./docs/05_Frontend_Documentation.md) — Documentação do frontend
- [`06_Prompt_Engineering_AI_Models.md`](./docs/06_Prompt_Engineering_AI_Models.md) — Engenharia de prompts
- [`07_Data_Model.md`](./docs/07_Data_Model.md) — Modelo de dados
- [`08_Deployment_Operations_Guide.md`](./docs/08_Deployment_Operations_Guide.md) — Deploy e operações
- [`09_User_Manual.md`](./docs/09_User_Manual.md) — Manual do usuário
- [`10_Test_Strategy_QA.md`](./docs/10_Test_Strategy_QA.md) — Estratégia de testes

---

## Contribuição

1. Abra uma branch a partir de `develop`
2. Implemente as mudanças seguindo os padrões do projeto
3. Certifique-se de que os testes passam (`./gradlew test` / `pnpm test`)
4. Abra um Pull Request para `develop`
