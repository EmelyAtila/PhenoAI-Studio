# 🌿 PhenoAI Studio
## Especificação de Design de API
> *API Design Specification (OpenAPI/REST)*

**Versão:** 1.0 | **Data:** Junho 2026 | **Equipe:** Neon Space Engineering Team

---


## 1. Convenções Gerais

### 1.1. Base URL e Versionamento

- **Produção:** `https://api.phenoai.studio/api/v1/`
- **Staging:** `https://api-staging.phenoai.studio/api/v1/`
- Versionamento via path (v1, v2) — backward compatibility garantida por 12 meses

### 1.2. Padrões de Request/Response

| Aspecto | Padrão Adotado |
|---|---|
| Formato de dados | JSON (application/json) em todas as requisições e respostas |
| Autenticação | Bearer Token JWT no header `Authorization: Bearer <token>` |
| Paginação | Query params: `page` (0-based), `size` (default 20, max 100), `sort` |
| Timestamps | ISO 8601 UTC: `2026-06-01T10:30:00Z` |
| IDs | UUID v4 em todos os recursos |
| Erros | RFC 7807 Problem Details: type, title, status, detail, instance |
| Nomenclatura de campos | camelCase em JSON |
| HTTP Status Codes | 200 OK, 201 Created, 204 No Content, 400, 401, 403, 404, 409, 422, 500 |

---

## 2. Auth Service — `/api/v1/auth`

| Método | Endpoint | Descrição | Auth |
|---|---|---|---|
| POST | `/auth/register` | Cadastro de novo usuário | Pública |
| POST | `/auth/login` | Login e emissão de JWT | Pública |
| POST | `/auth/refresh` | Renovação de access token via refresh token | Refresh Token |
| POST | `/auth/logout` | Invalidação do refresh token | Bearer JWT |
| POST | `/auth/forgot-password` | Solicitação de redefinição de senha | Pública |
| POST | `/auth/reset-password` | Redefinição de senha com token por email | Pública |

### POST /auth/register — Request Body

```json
{
  "name": "string",
  "email": "string (email)",
  "password": "string (min 8 chars)",
  "preferredLanguage": "pt-BR | en"
}
```

### POST /auth/login — Response 200

```json
{
  "accessToken": "string (JWT)",
  "refreshToken": "string",
  "expiresIn": 3600,
  "user": {
    "id": "uuid",
    "name": "string",
    "email": "string",
    "role": "RESEARCHER | ADMIN | GUEST"
  }
}
```

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
    "height_mm": 142.5,
    "width_mm": 98.3,
    "leafArea_cm2": 24.7,
    "dominantColor": "#3A7D44",
    "chlorophyllIndex": 0.82,
    "stressIndicators": ["mild_drought"],
    "overallHealth": "GOOD",
    "confidence": 0.94
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

Todas as respostas de erro seguem o formato **Problem Details for HTTP APIs (RFC 7807)**:

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
