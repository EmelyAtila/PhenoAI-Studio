# 🌿 PhenoAI Studio
## Documento de Visão e Escopo
> *Vision & Scope Document*

**Versão:** 1.0 | **Data:** Junho 2026 | **Equipe:** TechForge Senior Engineering Team

---


## 1. Visão Geral do Produto

O **PhenoAI Studio** é uma plataforma web de fenotipagem de plantas assistida por inteligência artificial, projetada para pesquisadores acadêmicos, agrônomos e instituições de pesquisa agrícola. A plataforma transforma o processo manual e laborioso de fenotipagem em um fluxo digital automatizado, preciso e escalável, integrando múltiplos provedores de IA (OpenAI, Anthropic, modelos locais) e fornecendo resultados estruturados em formato padronizado.

### 1.1. Declaração de Visão

> *"Capacitar pesquisadores e agrônomos com uma plataforma inteligente de fenotipagem de plantas que democratiza o acesso à IA multimodal, acelera descobertas científicas e suporta decisões agrícolas de alta precisão."*

### 1.2. Problema de Negócio

A fenotipagem manual de plantas é um processo lento, subjetivo e susceptível a erros humanos. Pesquisadores gastam horas analisando imagens individuais, sem padronização de resultados e sem capacidade de escalar análises para grandes volumes de amostras. O PhenoAI Studio resolve esse problema ao automatizar a análise via IA multimodal com outputs estruturados e reproduzíveis.

---

## 2. Stakeholders

| Stakeholder | Papel | Interesse Principal |
|---|---|---|
| Pesquisadores Acadêmicos | Usuário Final | Análises precisas, exportação de dados, multi-idioma |
| Agrônomos | Usuário Final | Detecção de pragas, estresse, diagnóstico rápido |
| Administradores do Sistema | Operacional | Gestão de usuários, monitoramento, configuração |
| TechForge Engineering Team | Desenvolvimento | Entrega técnica, qualidade, arquitetura |
| Gestores de Pesquisa | Decisor | ROI, relatórios, conformidade |

---

## 3. Escopo do Projeto

### 3.1. Dentro do Escopo (In-Scope)

- Interface web responsiva para upload e análise de imagens de plantas, folhas e sementes
- Três modos de análise por IA: Padrão, Customizada por Funcionalidade e Prompt Livre
- Integração com OpenAI (GPT-4o, GPT-4o-mini) e Anthropic (Claude 3.5 Sonnet, Opus)
- Gerenciamento seguro de credenciais de API por usuário (Credential Service)
- Catálogo dinâmico de modelos de IA (Model Catalog Service)
- Autenticação de usuários via OAuth2/JWT
- Histórico de análises por usuário com filtragem e paginação
- Exportação de relatórios em PDF e CSV
- Suporte multi-idioma: Português (BR) e Inglês
- Structured Outputs (JSON mode) para garantia de consistência das respostas
- Cache de respostas de IA via Redis para otimização de custo
- Arquitetura de microsserviços containerizada com Docker/Kubernetes

### 3.2. Fora do Escopo (Out-of-Scope) — Fase 1

- Integração com modelos locais (Ollama, LM Studio, vLLM) — previsto para Fase 3
- RAG (Retrieval Augmented Generation) — previsto para Fase 3
- Módulo de comparação avançada entre análises
- Anotação manual de imagens pelo usuário
- Integração com plataformas Hermes Agent e OpenClaw — avaliação na Fase 3
- App mobile nativo

---

## 4. Funcionalidades Principais

| ID | Funcionalidade | Prioridade | Fase |
|---|---|---|---|
| F01 | Autenticação e registro de usuários (OAuth2/JWT) | ALTA | Fase 1 |
| F02 | Upload de imagens com pré-visualização | ALTA | Fase 1 |
| F03 | Análise Fenotípica Padrão (prompt fixo otimizado) | ALTA | Fase 1 |
| F04 | Gerenciamento de credenciais de API (Credential Service) | ALTA | Fase 1 |
| F05 | Catálogo de modelos de IA disponíveis | ALTA | Fase 1 |
| F06 | Seleção de provedor/modelo/credencial na análise | ALTA | Fase 1 |
| F07 | Histórico de análises com paginação | ALTA | Fase 1 |
| F08 | Visualização estruturada de resultados | ALTA | Fase 1 |
| F09 | Análise Customizada por Funcionalidade | ALTA | Fase 2 |
| F10 | Prompt Livre pelo usuário | ALTA | Fase 2 |
| F11 | Integração com Anthropic Claude | ALTA | Fase 2 |
| F12 | Cache de respostas de IA (Redis) | MÉDIA | Fase 2 |
| F13 | Exportação de relatórios PDF/CSV | MÉDIA | Fase 2 |
| F14 | Suporte multi-idioma (PT-BR / EN) | MÉDIA | Fase 2 |
| F15 | Integração com modelos locais (Ollama/vLLM) | BAIXA | Fase 3 |
| F16 | RAG com base de conhecimento botânica | BAIXA | Fase 3 |

---

## 5. Critérios de Sucesso

1. Tempo médio de análise fenotípica inferior a **30 segundos** (excluindo latência da API de IA)
2. Disponibilidade do sistema igual ou superior a **99,5%** em ambiente de produção
3. **100%** dos resultados de IA em formato JSON estruturado válido conforme schema definido
4. Satisfação dos usuários pesquisadores igual ou superior a **4,0/5,0** em pesquisa pós-MVP
5. Cobertura de testes automatizados igual ou superior a **80%** no backend
6. **Zero** vulnerabilidades críticas de segurança identificadas no SAST/DAST pré-produção

---

## 6. Restrições e Premissas

### 6.1. Restrições

- As chaves de API de IA dos usuários devem ser criptografadas em repouso com **AES-256**
- A aplicação deve suportar ao menos **50 usuários concorrentes** no MVP sem degradação
- O sistema deve ser **cloud-agnostic** (AWS, GCP, Azure ou on-premise via Kubernetes)
- Conformidade com **LGPD** para dados de pesquisadores brasileiros

### 6.2. Premissas

- Os pesquisadores possuem suas próprias chaves de API OpenAI/Anthropic ou utilizam as do sistema
- As imagens enviadas são de plantas em condições razoáveis de iluminação e resolução mínima de **640×480px**
- A equipe de desenvolvimento terá acesso a ambientes de cloud para staging e produção
