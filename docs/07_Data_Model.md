# 🌿 PhenoAI Studio
## Modelo de Dados
> *Diagrama ER, Dicionário de Dados e Governança*

**Versão:** 1.1 | **Data:** Outubro 2026 | **Equipe:** Emely Atila Development

---


## 1. Visão Geral

O PhenoAI Studio utiliza **PostgreSQL 15+** como banco de dados principal, com suporte a **JSONB** para os resultados estruturados das análises de IA. O modelo é projetado seguindo os princípios de normalização (3FN), com uso de JSONB apenas onde a flexibilidade de schema supera o benefício da normalização.

### 1.1. Schemas por serviço

Todos os serviços compartilham o banco `phenoai`, mas cada um tem **seu próprio schema PostgreSQL** e roda as migrations Flyway isoladas nele (ver ADR-005 no SAD). Os schemas e as extensões `uuid-ossp` e `pg_trgm` são criados na inicialização do container por `backend/db/init/01_create_schemas.sql`.

| Schema | Serviço | Tabelas | Status |
|---|---|---|---|
| `auth` | Auth Service | `users` | Implementado |
| `users` | User Service | `user_profiles` | Implementado |
| `credentials` | Credential Service | `credentials` | Planejado |
| `catalog` | Model Catalog Service | `ai_models` | Planejado |
| `analysis` | Analysis Results Service | `plant_samples`, `images`, `analyses`, `analysis_results` | Planejado |
| — | AI Orchestration Service | — | Schema ainda não criado |

> As referências `FK → users.id` nas tabelas planejadas apontam para `auth.users.id`. Como cada tabela fica em um schema de outro serviço, ainda falta decidir se essas referências serão FKs físicas entre schemas ou apenas referências lógicas validadas pela aplicação.

---

## 2. Entidades e Dicionário de Dados

### 2.1. Tabela: `auth.users` — ✅ Implementada (`V1__create_user_table.sql`)

Guarda as credenciais de login. Os dados de perfil ficam em `users.user_profiles` (2.1A).

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK, DEFAULT uuid_generate_v4() | Identificador único do usuário |
| email | VARCHAR(255) | NOT NULL, UNIQUE | Email para login (também é o `sub` do JWT) |
| password | VARCHAR(255) | NOT NULL | Hash BCrypt da senha (`BCryptPasswordEncoder` com custo padrão 10) |
| role | VARCHAR(50) | NOT NULL, DEFAULT 'RESEARCHER' | Papel: ADMIN, RESEARCHER, GUEST (enum Java gravado como texto) |
| enabled | BOOLEAN | NOT NULL, DEFAULT TRUE | Conta ativa ou desativada |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Data e hora de cadastro |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Data e hora da última atualização |

> ⚠️ **Divergência:** a especificação original previa `name`, `preferred_language` e `last_login_at` nesta tabela, além de `TIMESTAMPTZ` nas datas. O nome foi para `users.user_profiles`. `preferred_language` e `last_login_at` ainda não existem, e as datas usam `TIMESTAMP` sem fuso.

### 2.1A. Tabela: `users.user_profiles` — ✅ Implementada (`V1__create_user_profile_table.sql`)

Perfil do usuário, mantido pelo User Service. A ligação com `auth.users` é feita pelo **email**, extraído do JWT.

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK, DEFAULT gen_random_uuid() | Identificador do perfil (diferente de `auth.users.id`) |
| email | VARCHAR(255) | NOT NULL, UNIQUE | Email do usuário (chave de ligação com `auth.users`) |
| name | VARCHAR(150) | NOT NULL | Nome completo |
| bio | TEXT | NULLABLE | Biografia (a API limita a 1000 caracteres) |
| organization | VARCHAR(255) | NULLABLE | Instituição/organização |
| role | VARCHAR(50) | NOT NULL, DEFAULT 'RESEARCHER' | Papel (não sincronizado com `auth.users.role`) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Data de criação do perfil |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Data da última atualização |

### 2.2. Tabela: `credentials`

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador único da credencial |
| user_id | UUID | FK → users.id, NOT NULL | Usuário proprietário da credencial |
| provider_name | ENUM | NOT NULL | OPENAI, ANTHROPIC, OLLAMA, VLLM |
| credential_name | VARCHAR(100) | NOT NULL | Nome amigável definido pelo usuário |
| encrypted_api_key | TEXT | NOT NULL | Chave de API criptografada (AES-256-GCM) |
| encryption_iv | VARCHAR(32) | NOT NULL | Vetor de inicialização para AES-GCM |
| status | ENUM | NOT NULL, DEFAULT ACTIVE | ACTIVE, INVALID, EXPIRED |
| last_validated_at | TIMESTAMPTZ | NULLABLE | Última validação bem-sucedida junto ao provedor |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Data de cadastro |

### 2.3. Tabela: `ai_models`

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador único do modelo |
| provider_name | ENUM | NOT NULL | OPENAI, ANTHROPIC, OLLAMA, VLLM |
| model_name | VARCHAR(100) | NOT NULL, UNIQUE | Identificador do modelo (ex: gpt-4o) |
| model_type | ENUM | NOT NULL | MULTIMODAL, TEXTUAL, LOCAL |
| input_cost_per_1k | DECIMAL(10,6) | NULLABLE | Custo por 1.000 tokens de entrada (USD) |
| output_cost_per_1k | DECIMAL(10,6) | NULLABLE | Custo por 1.000 tokens de saída (USD) |
| is_publicly_available | BOOLEAN | NOT NULL, DEFAULT true | Disponível para todos sem credencial própria |
| is_active | BOOLEAN | NOT NULL, DEFAULT true | Modelo habilitado no catálogo |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Última atualização dos metadados |

### 2.4. Tabela: `plant_samples`

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador único da amostra |
| user_id | UUID | FK → users.id, NOT NULL | Pesquisador responsável pela amostra |
| sample_name | VARCHAR(200) | NOT NULL | Nome identificador da amostra |
| plant_type | VARCHAR(100) | NULLABLE | Tipo/espécie de planta (ex: Glycine max) |
| location_lat | DECIMAL(9,6) | NULLABLE | Latitude geográfica da coleta |
| location_lng | DECIMAL(9,6) | NULLABLE | Longitude geográfica da coleta |
| additional_metadata | JSONB | NULLABLE | Metadados livres (condições, lote, etc.) |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Data de registro da amostra |

### 2.5. Tabela: `images`

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador único da imagem |
| plant_sample_id | UUID | FK → plant_samples.id, NOT NULL | Amostra à qual a imagem pertence |
| original_s3_url | TEXT | NOT NULL | URL S3 da imagem original (assinada internamente) |
| processed_s3_url | TEXT | NULLABLE | URL S3 da imagem pré-processada |
| original_filename | VARCHAR(255) | NOT NULL | Nome original do arquivo |
| mime_type | VARCHAR(50) | NOT NULL | Tipo MIME (image/jpeg, image/png, etc.) |
| file_size_bytes | BIGINT | NOT NULL | Tamanho do arquivo em bytes |
| width_px | INTEGER | NOT NULL | Largura da imagem em pixels |
| height_px | INTEGER | NOT NULL | Altura da imagem em pixels |
| content_hash | VARCHAR(64) | NOT NULL | SHA-256 do conteúdo para deduplicação e cache |
| captured_at | TIMESTAMPTZ | NULLABLE | Data e hora da captura (EXIF se disponível) |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Data de upload |

### 2.6. Tabela: `analyses`

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador único da análise |
| image_id | UUID | FK → images.id, NOT NULL | Imagem analisada |
| user_id | UUID | FK → users.id, NOT NULL | Usuário que solicitou a análise |
| ai_model_id | UUID | FK → ai_models.id, NOT NULL | Modelo de IA utilizado |
| credential_id | UUID | FK → credentials.id, NULLABLE | Credencial privada usada (NULL = sistema) |
| analysis_type | ENUM | NOT NULL | STANDARD, CUSTOM, FREE_PROMPT |
| selected_features | TEXT[] | NULLABLE | Array de features selecionadas |
| prompt_used | TEXT | NOT NULL | Prompt completo enviado à IA (auditoria) |
| prompt_version | VARCHAR(20) | NOT NULL | Versão do system prompt (ex: v1.0) |
| status | ENUM | NOT NULL, DEFAULT PENDING | PENDING, PROCESSING, COMPLETED, FAILED |
| total_tokens_used | INTEGER | NULLABLE | Total de tokens consumidos |
| estimated_cost_usd | DECIMAL(10,6) | NULLABLE | Custo estimado em USD |
| duration_ms | INTEGER | NULLABLE | Duração total do processamento em ms |
| error_message | TEXT | NULLABLE | Mensagem de erro se status = FAILED |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Data de solicitação |
| completed_at | TIMESTAMPTZ | NULLABLE | Data de conclusão |

### 2.7. Tabela: `analysis_results`

| Coluna | Tipo | Constraints | Descrição |
|---|---|---|---|
| id | UUID | PK | Identificador único do resultado |
| analysis_id | UUID | FK → analyses.id, NOT NULL, UNIQUE | Análise associada (1:1) |
| result_json | JSONB | NOT NULL | Resultado estruturado da IA (schema versionado) |
| schema_version | VARCHAR(20) | NOT NULL | Versão do schema JSON (ex: 1.0) |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Data de persistência do resultado |

---

## 3. Índices de Performance

| Tabela | Coluna(s) | Tipo | Justificativa |
|---|---|---|---|
| auth.users | email | UNIQUE B-TREE (+ `idx_users_email`, redundante com a constraint UNIQUE) | Login por email |
| users.user_profiles | email | UNIQUE B-TREE (+ `idx_user_profiles_email`, redundante) | Busca do perfil pelo email do JWT |
| credentials | user_id | B-TREE | Listagem de credenciais por usuário |
| analyses | user_id, created_at DESC | B-TREE COMPOSTO | Histórico paginado por usuário |
| analyses | status | B-TREE | Consulta de análises em processamento |
| images | content_hash | B-TREE | Deduplicação e geração de cache key |
| analysis_results | result_json | GIN | Queries sobre campos JSONB do resultado |
| plant_samples | user_id | B-TREE | Listagem de amostras por pesquisador |

---

## 4. Estratégia de Migrations e Governança

- **Ferramenta de migrations:** Flyway 9.22 (versão gerenciada pelo Spring Boot 3.2.5), executado automaticamente na subida de cada serviço. Não existe passo manual de `flywayMigrate`.
- **Localização:** `backend/<serviço>/src/main/resources/db/migration/`, com histórico próprio por schema (`<schema>.flyway_schema_history`)
- **Nomenclatura:** `V{n}__{descricao}.sql`, sequencial por serviço (ex: `V1__create_user_table.sql`, `V2__add_last_login.sql`). A numeração é independente entre serviços porque cada um tem o próprio schema.
- **SQL qualificado:** as migrations criam objetos com o schema explícito (`CREATE TABLE auth.users ...`), e as entidades JPA usam `@Table(schema = "...")`
- **Dependência:** não adicionar `org.flywaydb:flyway-database-postgresql`. Esse artefato só existe a partir do Flyway 10 e quebra a resolução de dependências.
- **Zero-downtime migrations:** adicionar colunas nullable, nunca remover/renomear sem backward compatibility
- **Schema versionado** na tabela `analysis_results.schema_version` para compatibilidade de leitura futura
- **Backup:** automatizado diário com retention de 30 dias; ponto de restauração em produção
- **Segurança:** dados sensíveis (encrypted_api_key, password_hash) nunca logados ou expostos em queries de debug

---

## 5. Relacionamentos (ERD Simplificado)

```
auth.users (1) ──── users.user_profiles (0..1)  [via email, sem FK]
users (1) ──< credentials (N)
users (1) ──< plant_samples (N)
users (1) ──< analyses (N)
plant_samples (1) ──< images (N)
images (1) ──< analyses (N)
ai_models (1) ──< analyses (N)
credentials (1) ──< analyses (N)  [NULLABLE]
analyses (1) ──── analysis_results (1)  [1:1]
```
