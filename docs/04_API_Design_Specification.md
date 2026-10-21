# 🌿 PhenoAI Studio
## Especificação de Design de API
> *API Design Specification (OpenAPI/REST)*

**Versão:** 1.1 | **Data:** Outubro 2026 | **Equipe:** Emely Atila Development

---

> **Status de implementação (v1.1):** só o **Auth Service** e o **User Service** estão implementados. As seções dos demais serviços continuam sendo a especificação-alvo. Nas seções implementadas, o conteúdo descreve o contrato real do código. Diferenças em relação ao padrão-alvo estão marcadas como **⚠️ Divergência**.

## 1. Convenções Gerais

### 1.1. Base URL e Versionamento

- **Produção (alvo):** `https://api.phenoai.studio/api/v1/`
- **Staging (alvo):** `https://api-staging.phenoai.studio/api/v1/`
- Versionamento via path (v1, v2) — backward compatibility garantida por 12 meses
- **Ambiente local (atual):** não há API Gateway nem prefixo `/api/v1`. Cada serviço é acessado diretamente pela porta dele, com `context-path` `/`. Exemplos: `http://localhost:8081/auth/login` e `http://localhost:8082/users/me`. O prefixo `/api/v1` será aplicado pelo API Gateway quando ele for implementado.
- **Swagger UI:** cada serviço expõe `/swagger-ui.html` e o JSON OpenAPI em `/v3/api-docs` (springdoc 2.5.0).

### 1.2. Padrões de Request/Response

| Aspecto | Padrão Adotado |
|---|---|
| Formato de dados | JSON (application/json) em todas as requisições e respostas |
| Autenticação | Bearer Token JWT no header `Authorization: Bearer <token>` |
| Paginação | Query params: `page` (0-based), `size` (default 20, max 100), `sort` |
| Timestamps | ISO 8601 UTC: `2026-06-01T10:30:00Z` |
| IDs | UUID v4 em todos os recursos |
| Erros | RFC 7807 Problem Details: type, title, status, detail, instance (alvo — ver seção 8 para o formato atual) |
| Nomenclatura de campos | camelCase em JSON |
| HTTP Status Codes | 200 OK, 201 Created, 204 No Content, 400, 401, 403, 404, 409, 422, 500 |

---

## 2. Auth Service — `/auth` (porta 8081) — ✅ Implementado

| Método | Endpoint | Descrição | Auth | Status |
|---|---|---|---|---|
| POST | `/auth/register` | Cadastro de novo usuário | Pública | ✅ |
| POST | `/auth/login` | Login e emissão de JWT | Pública | ✅ |
| POST | `/auth/refresh` | Renovação de access token | Refresh token no header `Authorization: Bearer <refreshToken>` | ✅ |
| POST | `/auth/logout` | Invalida o **access token** (blacklist no Redis até a expiração) | Bearer JWT (access token) | ✅ |
| POST | `/auth/forgot-password` | Solicitação de redefinição de senha | Pública | Planejado |
| POST | `/auth/reset-password` | Redefinição de senha com token por email | Pública | Planejado |

Todas as respostas do Auth Service usam o envelope `ApiResponse` do módulo `shared`. Campos nulos são omitidos.

```json
{ "success": true, "message": "string (opcional)", "data": { } }
```

### Tokens JWT

- Algoritmo HMAC-SHA (chave `JWT_SECRET`), `sub` = email do usuário, claims `role` (`ADMIN | RESEARCHER | GUEST`), `type` (`access | refresh`) e `jti` (UUID).
- Validade: access token `JWT_ACCESS_TOKEN_EXPIRATION` (padrão 3600 s) e refresh token `JWT_REFRESH_TOKEN_EXPIRATION` (padrão 604800 s = 7 dias).
- O `/auth/refresh` devolve um novo access token e **reaproveita o mesmo refresh token** (não há rotação).
- Novos usuários recebem o papel `RESEARCHER`.

### POST /auth/register — Request Body

```json
{
  "email": "string (email, obrigatório)",
  "password": "string (obrigatório, mín. 8 caracteres)"
}
```

> ⚠️ **Divergência:** a especificação original previa também `name` e `preferredLanguage` no cadastro (RF-01, RF-05). Hoje o nome é informado depois, no perfil do User Service (`PUT /users/me`), e a preferência de idioma ainda não é persistida.

### POST /auth/register — Response 201 · POST /auth/login e /auth/refresh — Response 200

```json
{
  "success": true,
  "message": "Usuário criado com sucesso",
  "data": {
    "accessToken": "string (JWT)",
    "refreshToken": "string (JWT)",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

`message` só vem preenchido no `/register`. A resposta não inclui o objeto `user`. Para obter os dados do usuário, use `GET /users/me` no User Service.

### POST /auth/logout — Response 200

```json
{ "success": true, "message": "Logout realizado com sucesso" }
```

### Códigos de erro do Auth Service

| Situação | HTTP | `message` |
|---|---|---|
| Falha de validação dos campos | 422 | mensagens dos campos separadas por vírgula |
| Email já cadastrado | 400 | `Email já cadastrado` |
| Refresh com token que não é do tipo `refresh` | 400 | `Token inválido` |
| Email ou senha incorretos | 401 | `Email ou senha inválidos` |
| Token malformado, com assinatura inválida ou expirado | 401 | `Token inválido ou expirado` |
| Erro inesperado | 500 | `Erro interno do servidor` |

> ⚠️ **Divergência:** pelo padrão-alvo, email duplicado deveria retornar **409 Conflict**. Hoje retorna 400.

---

## 2A. User Service — `/users` (porta 8082) — ✅ Implementado

Todas as rotas exigem `Authorization: Bearer <accessToken>` emitido pelo Auth Service. O serviço valida a assinatura com o mesmo `JWT_SECRET` e lê o email (`sub`) e o papel (`role`) direto do token.

| Método | Endpoint | Descrição | Auth |
|---|---|---|---|
| GET | `/users/me` | Retorna o perfil do usuário autenticado | Bearer JWT |
| PUT | `/users/me` | Cria ou atualiza (upsert) o perfil do usuário autenticado | Bearer JWT |
| GET | `/users` | Lista todos os perfis | Bearer JWT (ADMIN) |
| DELETE | `/users/{id}` | Remove um perfil por ID → 204 No Content | Bearer JWT (ADMIN) |

### PUT /users/me — Request Body

```json
{
  "name": "string (obrigatório, máx. 150)",
  "bio": "string (opcional, máx. 1000)",
  "organization": "string (opcional, máx. 255)"
}
```

### GET /users/me · PUT /users/me — Response 200

```json
{
  "id": "uuid",
  "email": "string",
  "name": "string",
  "bio": "string | null",
  "organization": "string | null",
  "role": "RESEARCHER | ADMIN | GUEST",
  "createdAt": "2026-06-01T10:30:00Z",
  "updatedAt": "2026-06-01T10:30:00Z"
}
```

> O `role` do perfil é gravado como `RESEARCHER` na criação e não é sincronizado com o papel do Auth Service. A autorização (`hasRole('ADMIN')`) usa o papel que vem no JWT.

### Códigos de erro do User Service

O User Service **não** usa o envelope `ApiResponse`. Os erros são retornados como `{ "error": "mensagem" }`.

| Situação | HTTP | `error` |
|---|---|---|
| Falha de validação | 400 | `campo: mensagem` (apenas o primeiro erro) |
| Perfil não encontrado (`GET /users/me` antes do primeiro `PUT`) | 404 | `Perfil não encontrado` |
| Usuário não encontrado no `DELETE` | 404 | `Usuário não encontrado` |
| Token ausente ou inválido | 403 | — (resposta padrão do Spring Security) |
| Papel diferente de ADMIN em rota administrativa | 403 | — |

> ⚠️ **Divergência:** Auth Service e User Service usam formatos de erro diferentes entre si, e nenhum dos dois segue o RFC 7807 (seção 8). O status de validação também difere: 422 no Auth e 400 no User.

---

## 3. Credential Service — `/api/v1/credentials`

| Método | Endpoint | Descrição | Auth |
|---|---|---|---|
| GET | `/credentials` | Lista credenciais do usuário autenticado (chaves mascaradas) | Bearer JWT |
| POST | `/credentials` | Cadastra nova credencial de API | Bearer JWT |
| GET | `/credentials/{id}` | Detalhe de uma credencial (mascarada) | Bearer JWT |
| PUT | `/credentials/{id}` | Atualiza nome ou chave de uma credencial | Bearer JWT |
| DELETE | `/credentials/{id}` | Remove uma credencial | Bearer JWT |
| POST | `/credentials/{id}/validate` | Valida a credencial junto ao provedor de IA | Bearer JWT |

### POST /credentials — Request Body

```json
{
  "providerName": "OPENAI | ANTHROPIC",
  "credentialName": "string (ex: Minha chave OpenAI)",
  "apiKey": "string (chave real, será criptografada)"
}
```

### GET /credentials — Response 200

```json
{
  "credentials": [
    {
      "id": "uuid",
      "providerName": "OPENAI",
      "credentialName": "Minha chave",
      "maskedKey": "sk-****abcd",
      "status": "ACTIVE | INVALID | EXPIRED",
      "createdAt": "2026-01-15T10:00:00Z"
    }
  ],
  "total": 3
}
```

---

## 4. Model Catalog Service — `/api/v1/models`

| Método | Endpoint | Descrição | Auth |
|---|---|---|---|
| GET | `/models` | Lista todos os modelos disponíveis (filtrável por provedor) | Bearer JWT |
| GET | `/models/{id}` | Detalhe de um modelo específico | Bearer JWT |
| POST | `/models` | Cadastra novo modelo (somente ADMIN) | Bearer JWT (ADMIN) |
| PUT | `/models/{id}` | Atualiza metadados de um modelo (somente ADMIN) | Bearer JWT (ADMIN) |

### GET /models — Response 200

```json
{
  "models": [
    {
      "id": "uuid",
      "providerName": "OPENAI",
      "modelName": "gpt-4o",
      "modelType": "MULTIMODAL",
      "inputCostPer1kTokens": 0.0025,
      "outputCostPer1kTokens": 0.01,
      "isPubliclyAvailable": true,
      "updatedAt": "2026-05-01T00:00:00Z"
    }
  ]
}
```

---

## 5. Upload Service — `/api/v1/uploads`

| Método | Endpoint | Descrição | Auth |
|---|---|---|---|
| POST | `/uploads/images` | Upload de imagem (multipart/form-data) | Bearer JWT |
| GET | `/uploads/images/{id}` | Metadados da imagem + URL de acesso temporária (S3 signed URL) | Bearer JWT |
| DELETE | `/uploads/images/{id}` | Remove imagem do sistema | Bearer JWT |

---

## 6. AI Orchestration Service — `/api/v1/analyses`

| Método | Endpoint | Descrição | Auth |
|---|---|---|---|
| POST | `/analyses` | Inicia nova análise fenotípica (assíncrono) | Bearer JWT |
| GET | `/analyses/{id}` | Consulta status e resultado de uma análise | Bearer JWT |
| GET | `/analyses` | Histórico de análises do usuário (paginado) | Bearer JWT |
| DELETE | `/analyses/{id}` | Remove análise e resultados associados | Bearer JWT |
| POST | `/analyses/{id}/re-run` | Re-executa análise com os mesmos parâmetros | Bearer JWT |

### POST /analyses — Request Body

```json
{
  "imageId": "uuid",
  "analysisType": "STANDARD | CUSTOM | FREE_PROMPT",
  "selectedFeatures": ["LEAF_AREA", "STRESS_DETECTION", "COLOR_ANALYSIS"],
  "freePrompt": "string (optional)",
  "aiProviderId": "uuid (model from catalog)",
  "credentialId": "uuid (optional, uses system if null)"
}
```

### POST /analyses — Response 202 Accepted

```json
{
  "analysisId": "uuid",
  "status": "PENDING",
  "estimatedDurationSeconds": 15,
  "statusCheckUrl": "/api/v1/analyses/{id}"
}
```

### GET /analyses/{id} — Response 200 (Completed)

```json
{
  "id": "uuid",
  "status": "COMPLETED",
  "analysisType": "STANDARD",
  "modelUsed": "gpt-4o",
  "providerUsed": "OPENAI",
  "durationMs": 8230,
  "tokenCost": 0.0124,
  "result": {
    "schemaVersion": "1.0",
    "plantType": "Soja",
    "growthStage": "VEGETATIVE",
    "morphometry": {
      "height_mm": 142.5,
      "width_mm": 98.3,
      "leafArea_cm2": 24.7,
      "aspectRatio": 1.45,
      "confidence": 0.94
    },
    "colorimetry": {
      "dominantColorHex": "#3A7D44",
      "chlorophyllIndex": 0.82,
      "colorUniformity": 0.88,
      "confidence": 0.91
    },
    "healthAssessment": {
      "overallHealth": "GOOD",
      "healthScore": 82,
      "stressIndicators": ["mild_drought"],
      "detectedDiseases": [],
      "detectedPests": [],
      "confidence": 0.89
    },
    "notes": "string",
    "analysisTimestamp": "2026-06-01T10:00:08Z"
  },
  "createdAt": "2026-06-01T10:00:00Z"
}
```

---

## 7. Reporting Service — `/api/v1/reports`

| Método | Endpoint | Descrição | Auth |
|---|---|---|---|
| POST | `/reports/pdf` | Gera relatório PDF de uma análise ou conjunto delas | Bearer JWT |
| POST | `/reports/csv` | Exporta resultados em CSV | Bearer JWT |
| GET | `/reports/{jobId}` | Status do job de geração do relatório | Bearer JWT |
| GET | `/reports/{jobId}/download` | Download do arquivo gerado | Bearer JWT |

---

## 8. Tratamento de Erros — Formato RFC 7807

> **Alvo, ainda não implementado.** Hoje o Auth Service responde com o envelope `ApiResponse` (`success: false`, `message`) e o User Service com `{ "error": "..." }` (ver seções 2 e 2A). A padronização no RFC 7807 deve acontecer antes do API Gateway, de preferência com um handler comum no módulo `shared`.

Todas as respostas de erro devem seguir o formato **Problem Details for HTTP APIs (RFC 7807)**:

```json
{
  "type": "https://phenoai.studio/errors/credential-invalid",
  "title": "Credencial de API Inválida",
  "status": 422,
  "detail": "A chave de API fornecida foi rejeitada pelo provedor OpenAI. Verifique se a chave está ativa.",
  "instance": "/api/v1/analyses",
  "traceId": "abc123def456"
}
```
