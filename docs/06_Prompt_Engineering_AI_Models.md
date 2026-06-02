# 🌿 PhenoAI Studio
## Engenharia de Prompts e Modelos de IA
> *Prompt Engineering, Structured Outputs e Gestão de Modelos*

**Versão:** 1.0 | **Data:** Junho 2026 | **Equipe:** TechForge Senior Engineering Team

---


## 1. Filosofia de Engenharia de Prompts

A qualidade das análises fenotípicas do PhenoAI Studio é diretamente determinada pela qualidade dos prompts enviados aos modelos de IA. A abordagem adotada segue os princípios de **Spec-Driven Prompting**: os prompts são tratados como especificações de software, versionados, testados e continuamente refinados com métricas objetivas de qualidade.

---

## 2. System Prompt — Análise Fenotípica Padrão (v1.0)

```
SYSTEM PROMPT — STANDARD_PHENOTYPING_V1

You are PhenoAI, an expert plant phenotyping assistant with deep expertise
in agronomy, plant biology, and computer vision. You analyze plant images
with scientific precision.

MANDATORY OUTPUT FORMAT: Respond ONLY with a valid JSON object matching
the provided schema. No text outside JSON.

ANALYSIS SCOPE: Morphometry (height, width, area), colorimetry (dominant
color, chlorophyll index), health assessment (stress indicators, diseases,
pests), growth stage, and overall phenotypic score.

CONFIDENCE: Report confidence (0.0-1.0) for each measurement. If image
quality is insufficient, set confidence < 0.5 and explain in notes.
```

---

## 3. Structured Output Schema — Análise Padrão (JSON Schema v1.0)

```json
{
  "schemaVersion": "1.0",
  "plantType": "string",
  "growthStage": "SEEDLING|VEGETATIVE|FLOWERING|FRUITING|MATURE",
  "morphometry": {
    "height_mm": "number",
    "width_mm": "number",
    "leafArea_cm2": "number",
    "aspectRatio": "number",
    "confidence": "number (0.0-1.0)"
  },
  "colorimetry": {
    "dominantColorHex": "string (#RRGGBB)",
    "chlorophyllIndex": "number (0.0-1.0)",
    "colorUniformity": "number (0.0-1.0)",
    "confidence": "number"
  },
  "healthAssessment": {
    "overallHealth": "EXCELLENT|GOOD|MODERATE|POOR|CRITICAL",
    "healthScore": "number (0-100)",
    "stressIndicators": ["string"],
    "detectedDiseases": ["string"],
    "detectedPests": ["string"],
    "confidence": "number"
  },
  "notes": "string",
  "analysisTimestamp": "ISO8601"
}
```

---

## 4. Funcionalidades de Análise Customizada

| Feature ID | Nome | Descrição | Campo no Schema |
|---|---|---|---|
| `LEAF_AREA` | Área Foliar | Estimativa da área foliar em cm² | `morphometry.leafArea_cm2` |
| `MORPHOMETRY` | Morfometria Completa | Altura, largura, proporção e forma | `morphometry.*` |
| `COLORIMETRY` | Colorimetria | Cor dominante, clorofila, uniformidade | `colorimetry.*` |
| `STRESS_DETECTION` | Detecção de Estresse | Seca, excesso de água, nutrição, UV | `healthAssessment.stressIndicators` |
| `DISEASE_DETECTION` | Detecção de Doenças | Manchas foliares, podridões, míldio | `healthAssessment.detectedDiseases` |
| `PEST_DETECTION` | Detecção de Pragas | Insetos, lagartas, ácaros, galhas | `healthAssessment.detectedPests` |
| `GROWTH_STAGE` | Estágio de Crescimento | Fase fenológica da planta | `growthStage` |
| `SEED_COUNT` | Contagem de Grãos | Estimativa do número de sementes/grãos | `seedCount` (schema opcional) |
| `FRUIT_COUNT` | Contagem de Frutos | Estimativa do número de frutos | `fruitCount` (schema opcional) |
| `ROOT_ANALYSIS` | Análise de Raízes | Comprimento, densidade, ramificação | `rootMetrics` (schema opcional) |

---

## 5. Prompt Builder — Análise Customizada (Strategy Pattern)

```java
// PromptBuilder.java
public String buildUserPrompt(Set<AnalysisFeature> features, String plantType) {
    StringBuilder sb = new StringBuilder();
    sb.append("Analyze this plant image.");
    if (plantType != null) sb.append(" Plant type: " + plantType + ".");
    sb.append(" Focus ONLY on: ");
    features.forEach(f -> sb.append(f.getDescription()).append("; "));
    sb.append("\nReturn ONLY JSON with fields: ");
    features.forEach(f -> sb.append(f.getSchemaFields()).append(", "));
    return sb.toString();
}
```

---

## 6. Catálogo de Modelos de IA

| Provedor | Modelo | Tipo | Multimodal | Custo Input/1K tokens | Custo Output/1K tokens | Uso Recomendado |
|---|---|---|---|---|---|---|
| OpenAI | gpt-4o | FRONTIER | Sim | US$ 0,0025 | US$ 0,0100 | Análises complexas, alta precisão |
| OpenAI | gpt-4o-mini | EFFICIENT | Sim | US$ 0,00015 | US$ 0,00060 | Volume alto, custo reduzido |
| Anthropic | claude-3-5-sonnet | FRONTIER | Sim | US$ 0,0030 | US$ 0,0150 | Análises descritivas detalhadas |
| Anthropic | claude-opus-4 | FRONTIER | Sim | US$ 0,0150 | US$ 0,0750 | Casos críticos de pesquisa |
| Local (Ollama) | llava | LOCAL | Sim | N/A | N/A | Ambiente local, privacidade total |
| Local (vLLM) | custom | LOCAL | Configurável | N/A | N/A | Infraestrutura dedicada, escala |

---

## 7. Estratégia de Cache e Otimização de Custos

### 7.1. Cache Key Composition

A chave de cache no Redis é composta por:

```
SHA256(imageContentHash + analysisType + sortedFeatures + modelId + providerId)
```

### 7.2. TTL por Tipo de Análise

| Tipo de Análise | TTL em Cache | Justificativa |
|---|---|---|
| Análise Padrão | 24 horas | Prompt fixo → resultado determinístico para mesma imagem |
| Análise Customizada | 12 horas | Features selecionadas podem mudar; reuso moderado |
| Prompt Livre | 1 hora | Alta variabilidade de prompts; menor probabilidade de reuso |

---

## 8. Versionamento de Prompts e Governança

- Todos os system prompts são versionados com semver (v1.0, v1.1, v2.0)
- Mudanças de major version (v1.x → v2.x) **invalidam o cache existente** automaticamente
- Cada análise armazena a versão do prompt utilizado no registro da `Analysis`
- Testes A/B de prompts são possíveis via feature flags no AI Orchestration Service
- **Métricas de qualidade:** precision rate, schema validation failure rate, user feedback score
