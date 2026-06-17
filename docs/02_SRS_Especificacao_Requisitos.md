# 🌿 PhenoAI Studio
## Especificação de Requisitos de Software
> *Software Requirements Specification (SRS)*

**Versão:** 1.0 | **Data:** Junho 2026 | **Equipe:** Neon Space Engineering Team

---


## 1. Introdução

### 1.1. Propósito

Este documento descreve os **requisitos funcionais e não funcionais** do sistema PhenoAI Studio, servindo como contrato técnico entre a equipe de desenvolvimento, stakeholders e usuários finais. Destina-se a desenvolvedores, arquitetos de software, equipe de QA e gestores de projeto.

### 1.2. Definições e Abreviações

| Termo | Definição |
|---|---|
| AI Orchestration Service | Microsserviço responsável pela seleção e comunicação com provedores de IA |
| Credential Service | Microsserviço de armazenamento seguro de chaves de API dos usuários |
| Model Catalog Service | Microsserviço que gerencia o catálogo de modelos disponíveis |
| Structured Output | Resposta da IA em formato JSON conforme schema predefinido |
| Fenotipagem | Análise de características observáveis de plantas (altura, área, cor, etc.) |
| JWT | JSON Web Token — mecanismo de autenticação stateless |
| RBAC | Role-Based Access Control — controle de acesso baseado em papéis |
| S3 | Amazon Simple Storage Service (ou compatível, ex.: MinIO) |

---

## 2. Requisitos Funcionais

### 2.1. Módulo de Autenticação e Usuários

| ID | Requisito | Prioridade |
|---|---|---|
| RF-01 | O sistema deve permitir cadastro de novos usuários com nome, email e senha | ALTA |
| RF-02 | O sistema deve autenticar usuários via JWT com refresh token | ALTA |
| RF-03 | O sistema deve suportar recuperação de senha via email | MÉDIA |
| RF-04 | O sistema deve implementar papéis de usuário: ADMIN, RESEARCHER, GUEST | ALTA |
| RF-05 | O sistema deve permitir ao usuário editar seu perfil e preferências de idioma | BAIXA |

### 2.2. Módulo de Gerenciamento de Credenciais de IA

| ID | Requisito | Prioridade |
|---|---|---|
| RF-10 | O sistema deve permitir ao usuário cadastrar chaves de API para OpenAI e Anthropic | ALTA |
| RF-11 | As chaves de API devem ser armazenadas criptografadas (AES-256) no banco de dados | ALTA |
| RF-12 | O sistema deve exibir as chaves mascaradas (ex.: sk-****abcd) na interface | ALTA |
| RF-13 | O sistema deve permitir ao usuário nomear, editar e remover credenciais | ALTA |
| RF-14 | O sistema deve validar a credencial junto ao provedor ao cadastrá-la | MÉDIA |
| RF-15 | O sistema deve exibir o status da credencial: ATIVA, INVÁLIDA, EXPIRADA | MÉDIA |

### 2.3. Módulo de Upload e Processamento de Imagens

| ID | Requisito | Prioridade |
|---|---|---|
| RF-20 | O sistema deve aceitar uploads de imagens nos formatos JPEG, PNG, TIFF e WebP | ALTA |
| RF-21 | O tamanho máximo por imagem deve ser de 20 MB | ALTA |
| RF-22 | O sistema deve exibir pré-visualização da imagem após upload | ALTA |
| RF-23 | O sistema deve redimensionar e normalizar a imagem antes do envio à IA | MÉDIA |
| RF-24 | As imagens devem ser armazenadas no S3 com URL assinada para acesso seguro | ALTA |
| RF-25 | O usuário deve poder associar metadados à imagem: tipo de planta, nome da amostra, data | MÉDIA |

### 2.4. Módulo de Análise por IA

| ID | Requisito | Prioridade |
|---|---|---|
| RF-30 | O sistema deve suportar Análise Fenotípica Padrão com prompt otimizado fixo | ALTA |
| RF-31 | O sistema deve suportar Análise Customizada com seleção de funcionalidades pelo usuário | ALTA |
| RF-32 | O sistema deve suportar Prompt Livre inserido pelo usuário | ALTA |
| RF-33 | O usuário deve poder selecionar o provedor de IA (OpenAI, Anthropic) para cada análise | ALTA |
| RF-34 | O usuário deve poder selecionar o modelo específico a partir do catálogo | ALTA |
| RF-35 | O usuário deve poder optar por usar sua credencial privada ou a do sistema | ALTA |
| RF-36 | As respostas de IA devem retornar em JSON estruturado conforme schema versionado | ALTA |
| RF-37 | O sistema deve processar análises de forma assíncrona com status em tempo real | ALTA |
| RF-38 | O sistema deve implementar cache de respostas para análises idênticas (Redis) | MÉDIA |

### 2.5. Módulo de Resultados, Histórico e Exportação

| ID | Requisito | Prioridade |
|---|---|---|
| RF-40 | O sistema deve exibir resultados de análise de forma estruturada e visual | ALTA |
| RF-41 | O sistema deve manter histórico completo de análises por usuário | ALTA |
| RF-42 | O usuário deve poder filtrar o histórico por data, tipo de análise e status | MÉDIA |
| RF-43 | O sistema deve permitir exportação de resultados em PDF com layout profissional | MÉDIA |
| RF-44 | O sistema deve permitir exportação de resultados em CSV para análise em planilhas | MÉDIA |
| RF-45 | O sistema deve permitir re-executar uma análise anterior com os mesmos parâmetros | BAIXA |

---

## 3. Requisitos Não Funcionais

### 3.1. Desempenho

| ID | Requisito | Meta |
|---|---|---|
| RNF-01 | Tempo de resposta da API (excluindo IA externa) | < 500ms p95 |
| RNF-02 | Tempo de pré-processamento de imagem | < 2s |
| RNF-03 | Throughput de análises simultâneas (MVP) | ≥ 50 usuários concorrentes |
| RNF-04 | Disponibilidade do sistema | ≥ 99,5% uptime mensal |
| RNF-05 | Tempo de carregamento inicial da aplicação web (LCP) | < 2,5s |

### 3.2. Segurança

| ID | Requisito | Padrão |
|---|---|---|
| RNF-10 | Criptografia de chaves de API em repouso | AES-256 |
| RNF-11 | Comunicação entre serviços e clientes | TLS 1.3 |
| RNF-12 | Controle de acesso baseado em papéis | RBAC com JWT |
| RNF-13 | Proteção contra OWASP Top 10 | SAST + DAST obrigatório |
| RNF-14 | Gerenciamento de segredos de infraestrutura | HashiCorp Vault ou K8s Secrets |
| RNF-15 | Conformidade de privacidade de dados | LGPD (Lei 13.709/2018) |

### 3.3. Manutenibilidade e Escalabilidade

- Arquitetura de microsserviços com separação de responsabilidades por domínio
- Versionamento de APIs REST (ex.: /api/v1/, /api/v2/)
- Cobertura de testes automatizados ≥ 80% (backend) e ≥ 70% (frontend)
- Escalabilidade horizontal via Kubernetes HPA para todos os microsserviços críticos
- Uso de feature flags para lançamento gradual de funcionalidades

---

## 4. Histórias de Usuário — Épicos Principais

### Épico 1: Autenticação e Identidade

- Como pesquisador, quero me **cadastrar** com email e senha para ter acesso personalizado à plataforma
- Como usuário, quero fazer **login** com segurança e manter minha sessão ativa por tempo configurável
- Como usuário, quero **recuperar minha senha** por email em caso de esquecimento

### Épico 2: Gestão de Credenciais de IA

- Como pesquisador, quero **cadastrar minha chave de API OpenAI** para usar minha própria cota
- Como pesquisador, quero **nomear e gerenciar múltiplas credenciais** por provedor
- Como pesquisador, quero **saber se minha credencial está válida** antes de iniciar uma análise

### Épico 3: Análise Fenotípica

- Como agrônomo, quero **enviar uma foto de folha** e receber análise automática de área, cor e sintomas
- Como pesquisador, quero **selecionar quais características analisar** (customizado) para economizar tokens
- Como usuário avançado, quero **escrever meu próprio prompt** para análises exploratórias
- Como usuário, quero **escolher qual modelo e provedor de IA** usar para cada análise

### Épico 4: Resultados e Relatórios

- Como pesquisador, quero **visualizar os resultados** da análise de forma clara e estruturada
- Como pesquisador, quero **exportar os resultados em PDF** para incluir em meu relatório científico
- Como pesquisador, quero **exportar os dados em CSV** para análise estatística no R ou Python
- Como usuário, quero **visualizar o histórico** de todas as minhas análises anteriores
