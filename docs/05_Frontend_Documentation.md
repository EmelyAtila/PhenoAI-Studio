# 🌿 PhenoAI Studio
## Documentação de Frontend
> *Design System, Componentes e Diretrizes UI/UX*

**Versão:** 1.0 | **Data:** Junho 2026 | **Equipe:** Neon Space Engineering Team

---


## 1. Stack Tecnológico

| Tecnologia | Versão | Propósito |
|---|---|---|
| Next.js | 14.x (App Router) | Framework React com SSR, SSG e Server Components |
| React | 18.x | Biblioteca de UI com Concurrent Features |
| TypeScript | 5.x | Tipagem estática para maior confiabilidade |
| Tailwind CSS | 3.x | Utility-first CSS com design system customizável |
| Zustand | 4.x | Gerenciamento de estado global leve e performático |
| TanStack Query | 5.x (React Query) | Cache, sync e estado de dados remotos |
| React Hook Form | 7.x | Gerenciamento de formulários com validação |
| Zod | 3.x | Validação e inferência de tipos em runtime |
| i18next + next-i18next | Latest | Internacionalização (PT-BR / EN) |
| Playwright | Latest | Testes E2E automatizados |
| React Testing Library | Latest | Testes unitários de componentes |
| Storybook | 7.x | Documentação viva de componentes |

---

## 2. Design System — Tokens

### 2.1. Paleta de Cores

| Token | Valor HEX | Uso |
|---|---|---|
| `--color-primary-900` | `#1B5E20` | Headers, CTAs principais, ícones de destaque |
| `--color-primary-700` | `#2E7D32` | Botões primários, links |
| `--color-primary-300` | `#81C784` | Hover states, bordas de foco |
| `--color-primary-50` | `#E8F5E9` | Backgrounds de cards, zebra de tabelas |
| `--color-accent` | `#66BB6A` | Badges, tags, indicadores de sucesso |
| `--color-warning` | `#FFA726` | Alertas, status amarelo |
| `--color-danger` | `#EF5350` | Erros, status vermelho, exclusão |
| `--color-neutral-900` | `#212121` | Texto principal |
| `--color-neutral-600` | `#616161` | Texto secundário, placeholders |
| `--color-neutral-100` | `#F5F5F5` | Background da aplicação |

### 2.2. Tipografia

| Variante | Fonte | Tamanho | Peso | Uso |
|---|---|---|---|---|
| Display | Inter | 2.25rem / 36px | 700 Bold | Títulos de página principais |
| Heading 1 | Inter | 1.875rem / 30px | 700 Bold | Seções principais |
| Heading 2 | Inter | 1.5rem / 24px | 600 SemiBold | Subseções |
| Heading 3 | Inter | 1.25rem / 20px | 600 SemiBold | Cards, labels de grupo |
| Body Large | Inter | 1.125rem / 18px | 400 Regular | Texto de destaque, leads |
| Body | Inter | 1rem / 16px | 400 Regular | Texto padrão |
| Body Small | Inter | 0.875rem / 14px | 400 Regular | Labels, metadata |
| Caption | Inter | 0.75rem / 12px | 400 Regular | Tooltips, timestamps |
| Code | JetBrains Mono | 0.875rem / 14px | 400 Regular | JSON output, prompts |

---

## 3. Componentes Principais

### 3.1. Componentes de Layout

- **AppShell:** layout raiz com Sidebar + Header + Content Area, responsivo
- **Sidebar:** navegação principal colapsável, com indicador de rota ativa
- **Header:** breadcrumb, seletor de idioma, avatar do usuário com menu dropdown
- **PageContainer:** padding, max-width e heading padronizados por página

### 3.2. ImageUploader

Componente de upload com drag-and-drop, validação de tipo/tamanho, barra de progresso, pré-visualização imediata.

| Estado | Comportamento |
|---|---|
| `idle` | Área de drop com ícone e instrução textual |
| `dragging` | Borda animada verde com feedback visual |
| `uploading` | Progress bar com porcentagem e cancel button |
| `success` | Thumbnail + checkmark + nome do arquivo + botão de remoção |
| `error` | Mensagem de erro inline com retry action |

### 3.3. AnalysisConfigurator

Componente de configuração da análise. Renderiza dinamicamente baseado no `analysisType` selecionado.

- **Tab 1 — Análise Padrão:** apenas seleção de modelo e credencial
- **Tab 2 — Análise Customizada:** checkboxes de funcionalidades + seleção de modelo
- **Tab 3 — Prompt Livre:** textarea com markdown hints + seleção de modelo
- **Seção Configuração de IA:** ProviderSelector → ModelSelector → CredentialSelector (cascata)

### 3.4. CredentialManager

Página dedicada para CRUD de credenciais de IA. Implementa mascaramento de chave (nunca exibe completa).

- Lista de credenciais com badge de status (ACTIVE/INVALID), última validação, botões de editar/remover
- Modal de adição: provider select, nome amigável, campo de API key (password type), botão "Validar e Salvar"
- Indicador visual de status: 🟢 verde (ACTIVE), 🔴 vermelho (INVALID), 🟡 amarelo (EXPIRADO)

### 3.5. AnalysisResultViewer

Componente de exibição de resultados estruturados. Renderiza o JSON em painéis temáticos.

- **Painel Morfometria:** altura, largura, área foliar com barras de progresso visuais
- **Painel Colorimetria:** círculo de cor dominante, índice de clorofila, paleta de cores
- **Painel Saúde:** indicadores de estresse, score de saúde geral, badges de diagnóstico
- **Painel Metadados:** modelo usado, provedor, tokens, custo estimado, duração
- **Ações:** Download PDF, Download CSV, Re-executar análise

### 3.6. AnalysisHistory

Tabela paginada do histórico de análises com TanStack Query para cache e refetch automático.

- **Colunas:** thumbnail miniatura, tipo de análise, modelo IA, status, data, ações
- **Filtros:** date range picker, tipo de análise, status, provedor de IA
- **Row actions:** ver resultado, re-executar, exportar, remover

---

## 4. Páginas da Aplicação

| Rota | Página | Renderização | Auth |
|---|---|---|---|
| `/auth/login` | Login | SSR | Pública |
| `/auth/register` | Cadastro | SSR | Pública |
| `/auth/forgot-password` | Recuperar Senha | SSR | Pública |
| `/` | Dashboard | SSR + Client | Autenticado |
| `/analyses/new` | Nova Análise (Upload + Config) | Client | Autenticado |
| `/analyses` | Histórico de Análises | SSR + Client | Autenticado |
| `/analyses/[id]` | Resultado da Análise | SSR + Client | Autenticado |
| `/settings/credentials` | Gerenciar Credenciais de IA | Client | Autenticado |
| `/settings/profile` | Perfil do Usuário | Client | Autenticado |
| `/admin/models` | Catálogo de Modelos (Admin) | Client | ADMIN |

---

## 5. Internacionalização (i18n)

- **Idiomas suportados:** Português Brasileiro (`pt-BR`, padrão) e Inglês (`en`)
- Arquivos de tradução em `/public/locales/{lang}/*.json` por domínio (auth, analysis, common, etc.)
- Seletor de idioma no Header com persistência via cookie/localStorage
- URLs com locale prefix opcional: `/pt-BR/analyses` ou `/en/analyses`
- Datas, moedas e números formatados via `Intl` API conforme o locale ativo

---

## 6. Performance e Core Web Vitals

| Métrica | Meta | Estratégia |
|---|---|---|
| LCP (Largest Contentful Paint) | < 2,5s | Image optimization, preload de hero, Server Components |
| INP (Interaction to Next Paint) | < 200ms | useTransition, defer non-critical updates, Zustand slice |
| CLS (Cumulative Layout Shift) | < 0,1 | Skeleton placeholders, reserva de espaço para imagens |
| TTFB (Time to First Byte) | < 800ms | SSR com cache de dados, CDN para assets estáticos |

---

## 7. Diretrizes de Acessibilidade (WCAG 2.1 AA)

- **Contraste de cores:** mínimo 4,5:1 para texto normal, 3:1 para texto grande
- **Navegação por teclado:** todos os elementos interativos acessíveis via Tab/Enter/Space/Esc
- **Screen readers:** aria-label, aria-describedby, role apropriados em todos os componentes custom
- **Feedback de formulários:** erros de validação vinculados ao campo via aria-describedby
- **Imagens:** alt text descritivo em todas as imagens funcionais; `alt=""` para decorativas
- **Modais e drawers:** foco gerenciado (focus trap), Escape fecha, foco retorna ao trigger
