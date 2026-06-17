# 🌿 PhenoAI Studio
## Estratégia de Testes e Qualidade
> *Test Strategy, QA Plan e Quality Gates*

**Versão:** 1.0 | **Data:** Junho 2026 | **Equipe:** Neon Space Engineering Team

---


## 1. Filosofia de Qualidade

O PhenoAI Studio adota a abordagem **Shift-Left Testing**: testes são parte integral do desenvolvimento, não uma etapa final. A **pirâmide de testes** guia a estratégia: base larga de testes unitários, camada intermediária de testes de integração e topo com testes E2E focados em fluxos críticos.

---

## 2. Pirâmide de Testes — Distribuição

| Nível | Quantidade | % do Total | Ferramentas | Executado em |
|---|---|---|---|---|
| Unitário | Alta (~70%) | 70% | JUnit 5, Mockito, React Testing Library, Jest | Cada commit/PR |
| Integração | Média (~20%) | 20% | Testcontainers, Spring Boot Test, MockMvc | Cada commit/PR |
| Componente/Serviço | Baixa (~7%) | 7% | REST Assured, Pact (Consumer-Driven Contracts) | Push → develop |
| E2E | Mínima (~3%) | 3% | Playwright, Cypress | Pre-deploy staging/prod |
| Performance/Carga | Pontual | Suporte | JMeter, Gatling | Sprint de release |
| Segurança | Pontual | Suporte | SonarQube (SAST), OWASP ZAP (DAST), Trivy | CI/CD + trimestral |

---

## 3. Testes Unitários — Backend (Java/Spring)

### 3.1. Padrão de Nomenclatura e Estrutura (Given/When/Then)

```java
@Test
@DisplayName("should encrypt API key before saving to database")
void shouldEncryptApiKeyBeforeSaving() {
    // GIVEN
    var request = new CreateCredentialRequest("OPENAI", "My Key", "sk-real-key-123");

    // WHEN
    var saved = credentialService.create(userId, request);

    // THEN
    assertThat(saved.getEncryptedApiKey()).isNotEqualTo("sk-real-key-123");
    assertThat(saved.getStatus()).isEqualTo(CredentialStatus.ACTIVE);
    verify(encryptionService).encrypt("sk-real-key-123");
}
```

### 3.2. Casos de Teste Críticos — AI Orchestration Service

| Caso de Teste | Cenário | Resultado Esperado |
|---|---|---|
| TC-AI-001 | Seleção de provedor OpenAI com credencial privada válida | OpenAIStrategy instanciada com a chave correta |
| TC-AI-002 | Seleção de provedor Anthropic com credencial inválida | CredentialInvalidException lançada, análise não iniciada |
| TC-AI-003 | Provedor null → usa credencial de sistema configurada | SystemCredentialFallback ativado |
| TC-AI-004 | Resposta da IA com JSON inválido (não conforme schema) | StructuredOutputValidationException + análise FAILED |
| TC-AI-005 | Cache hit para análise idêntica | Retorno do cache sem chamar a API de IA |
| TC-AI-006 | Timeout na API de IA externa (> 60s) | CircuitBreaker abre, FAILED com mensagem amigável |
| TC-AI-007 | Construção do prompt customizado com 3 features | Prompt contém exatamente as 3 features selecionadas |
| TC-AI-008 | Análise Padrão com prompt versão v1.0 | `system_prompt_version = "1.0"` registrado na análise |

### 3.3. Casos de Teste Críticos — Credential Service

| Caso de Teste | Cenário | Resultado Esperado |
|---|---|---|
| TC-CRED-001 | Salvar credencial — chave não pode ser lida em texto puro no banco | encrypted_api_key != original_key no PostgreSQL |
| TC-CRED-002 | Descriptografar credencial para uso interno | decrypted_key == original_key no serviço |
| TC-CRED-003 | Acesso não autorizado ao Credential Service (sem JWT válido) | HTTP 401 Unauthorized |
| TC-CRED-004 | Usuário tentando acessar credencial de outro usuário | HTTP 403 Forbidden |
| TC-CRED-005 | Validação de credencial junto ao provedor — chave válida | status atualizado para ACTIVE |
| TC-CRED-006 | Validação de credencial junto ao provedor — chave inválida | status atualizado para INVALID |

---

## 4. Testes de Integração — Testcontainers

```java
@Testcontainers
@SpringBootTest
class AnalysisIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15");

    @Container
    static GenericContainer<?> redis =
        new GenericContainer<>("redis:7").withExposedPorts(6379);

    @Container
    static KafkaContainer kafka =
        new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    // Testa o fluxo completo: Upload → ImageProcessing → AIOrchestration → Results
}
```

---

## 5. Testes E2E — Playwright

### 5.1. Fluxos Críticos Cobertos

| Fluxo E2E | Steps | Prioridade |
|---|---|---|
| E2E-001: Happy Path Completo | Login → Upload imagem → Análise Padrão → Visualizar resultado → Export PDF | CRÍTICA |
| E2E-002: Gestão de Credenciais | Login → Settings → Add credential → Validate → Use in analysis | ALTA |
| E2E-003: Análise Customizada | Login → Upload → Select 3 features → Select model → Run → View result | ALTA |
| E2E-004: Prompt Livre | Login → Upload → Enter free prompt → Run → View result | ALTA |
| E2E-005: Histórico e Filtros | Login → History → Filter by date → Filter by status → Re-run analysis | MÉDIA |
| E2E-006: Export CSV | Login → History → Select analysis → Export CSV → Verify download | MÉDIA |
| E2E-007: Credencial Inválida | Login → Add invalid credential → Verify INVALID status → Analysis blocked | ALTA |
| E2E-008: Acessibilidade | Navegação completa por teclado em todos os fluxos críticos (axe-core) | MÉDIA |

---

## 6. Quality Gates — CI/CD

| Gate | Métrica | Threshold | Ação se Falhar |
|---|---|---|---|
| Code Coverage Backend | Cobertura de testes unitários | ≥ 80% | Bloqueia merge do PR |
| Code Coverage Frontend | Cobertura de testes componentes | ≥ 70% | Bloqueia merge do PR |
| SAST Bugs Críticos | Bugs críticos no SonarQube | = 0 | Bloqueia merge do PR |
| SAST Vulnerabilidades | Vulnerabilidades de segurança | = 0 críticas/altas | Bloqueia merge do PR |
| Duplicidade de Código | Código duplicado no SonarQube | < 5% | Warning (não bloqueia) |
| E2E Pass Rate | Testes E2E passando em staging | = 100% | Bloqueia deploy para produção |
| Performance LCP | Largest Contentful Paint | < 2,5s | Warning com relatório no PR |
| Container Vulnerabilities | CVEs críticos via Trivy | = 0 críticas | Bloqueia build da imagem |

---

## 7. Testes de Performance — JMeter/Gatling

### 7.1. Cenário de Carga — Análise Fenotípica (MVP)

- **Usuários simultâneos:** 50 (crescimento gradual de 0 a 50 em 5 minutos)
- **Duração:** 30 minutos de carga sustentada
- **Cenário:** 70% análise padrão, 20% customizada, 10% prompt livre
- **SLA:** p95 latência API < 500ms (excluindo chamada de IA externa), zero erros HTTP 5xx
- **Throughput mínimo:** 10 análises por minuto concluídas end-to-end

---

## 8. Testes de Segurança

| Tipo | Ferramenta | Frequência | Foco Principal |
|---|---|---|---|
| SAST | SonarQube + SpotBugs | Cada PR | SQL Injection, OWASP Top 10, criptografia insegura |
| DAST | OWASP ZAP (automatizado) | Pré-deploy staging | XSS, CSRF, headers de segurança, JWT |
| Dependency Scan | Dependabot + OWASP Dependency-Check | Diário | CVEs em dependências |
| Container Scan | Trivy | Cada build de imagem | CVEs em imagens base e pacotes OS |
| Secret Detection | git-secrets + truffleHog | Pre-commit hook + CI | Chaves de API no código |
| Penetration Testing | Equipe especializada externa | Pré-launch + semestral | Credential Service, JWT, S3 |

---

## 9. Processo de QA no Ciclo de Desenvolvimento

1. **Refinamento de histórias:** QA participa da definição dos critérios de aceite e casos de borda
2. **Desenvolvimento:** Desenvolvedor escreve testes unitários junto com o código (TDD para lógica crítica)
3. **Pull Request:** CI executa todos os testes; quality gates devem passar para merge
4. **Sprint Review:** QA realiza teste exploratório das novas funcionalidades em staging
5. **Pre-release:** execução completa dos testes E2E, performance e segurança DAST
6. **Pós-deploy em produção:** monitoramento ativo por 30 minutos com alertas configurados
