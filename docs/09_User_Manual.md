# 🌿 PhenoAI Studio
## Manual do Usuário
> *Guia Completo de Utilização do PhenoAI Studio*

**Versão:** 1.0 | **Data:** Junho 2026 | **Equipe:** Neon Space Engineering Team

---


## 1. Bem-vindo ao PhenoAI Studio

O **PhenoAI Studio** é uma plataforma de fenotipagem de plantas assistida por inteligência artificial. Com ele, você pode enviar imagens de plantas, folhas ou sementes e obter análises científicas detalhadas em segundos, utilizando os mais avançados modelos de IA disponíveis.

---

## 2. Primeiros Passos — Criando sua Conta

1. Acesse **https://phenoai.studio** no seu navegador (Chrome, Firefox, Edge ou Safari, versão atual)
2. Clique em **"Criar conta"** na página inicial
3. Preencha seu nome, email profissional e defina uma senha com ao menos **8 caracteres**
4. Selecione seu idioma preferido (Português ou English) e clique em **"Criar conta"**
5. Você receberá um **email de confirmação**. Clique no link para ativar sua conta
6. Após a ativação, faça login com seu email e senha

---

## 3. Configurando suas Chaves de API (Credenciais de IA)

Para usar sua própria cota de IA (recomendado para pesquisadores com alto volume de análises):

1. No menu lateral, clique em **"Configurações"** → **"Credenciais de IA"**
2. Clique no botão **"+ Adicionar Credencial"**
3. Selecione o **Provedor:** OpenAI ou Anthropic
4. Dê um **nome amigável** à credencial (ex: "Minha chave OpenAI - Projeto Soja")
5. Cole sua **chave de API** no campo correspondente (começa com `sk-` para OpenAI)
6. Clique em **"Validar e Salvar"** — o sistema verificará se a chave está ativa
7. Credenciais válidas aparecerão com o indicador 🟢 **"ATIVA"**

> ⚠️ **SEGURANÇA:** Sua chave de API é armazenada de forma **criptografada** e nunca será exibida por completo. Nunca compartilhe sua chave com terceiros. Caso suspeite de comprometimento, remova a credencial e gere uma nova chave no painel do provedor.

---

## 4. Realizando sua Primeira Análise

### 4.1. Upload de Imagem

1. No menu lateral, clique em **"Nova Análise"**
2. Na área de upload, **arraste e solte** sua imagem ou clique para selecionar do computador
3. **Formatos aceitos:** JPEG, PNG, TIFF, WebP | **Tamanho máximo:** 20 MB
4. Uma pré-visualização da imagem aparecerá. Preencha opcionalmente: Nome da amostra, Tipo de planta
5. Clique em **"Próximo: Configurar Análise"**

### 4.2. Configurando o Tipo de Análise

#### Opção A — Análise Fenotípica Padrão *(Recomendado para iniciantes)*

Realiza uma análise completa e abrangente: morfometria, colorimetria, saúde e estágio de crescimento.

- Selecione a aba **"Análise Padrão"**
- Escolha o **Provedor** (ex: OpenAI) e o **Modelo** (ex: GPT-4o para maior precisão)
- Opcionalmente selecione sua **credencial privada** ou use a do sistema
- Clique em **"Iniciar Análise"**

#### Opção B — Análise Customizada por Funcionalidade

Ideal para pesquisadores que precisam de análises específicas e desejam otimizar o consumo de tokens.

- Selecione a aba **"Análise Customizada"**
- Marque as funcionalidades desejadas:
  - ☐ Área Foliar
  - ☐ Morfometria Completa
  - ☐ Colorimetria
  - ☐ Detecção de Estresse
  - ☐ Detecção de Doenças
  - ☐ Detecção de Pragas
  - ☐ Estágio de Crescimento
  - ☐ Contagem de Grãos
  - ☐ Contagem de Frutos
- Selecione o modelo de IA e credencial → Clique em **"Iniciar Análise"**

#### Opção C — Prompt Livre *(Para usuários avançados)*

- Selecione a aba **"Prompt Livre"**
- Digite sua pergunta ou instrução (ex: *"Estime a porcentagem de área foliar afetada por mancha angular e indique a severidade segundo a escala de 1 a 5"*)
- Selecione o modelo e credencial → Clique em **"Iniciar Análise"**

### 4.3. Acompanhando o Processamento

Após iniciar, você verá o status em tempo real:

> **Recebido** → **Processando Imagem** → **Consultando IA** → **Concluído**

O tempo médio é de **10 a 30 segundos**, dependendo do modelo e da complexidade da análise.

---

## 5. Visualizando os Resultados

Ao concluir, os resultados são exibidos em painéis organizados:

| Painel | Conteúdo |
|---|---|
| 🌿 Morfometria | Altura, largura, área foliar estimada com barra visual |
| 🎨 Colorimetria | Paleta de cor dominante, índice de clorofila (0.0 a 1.0) |
| 💚 Saúde | Score geral, indicadores de estresse, doenças e pragas detectadas |
| ℹ️ Metadados | Modelo utilizado, provedor, tokens consumidos, custo estimado, duração |

---

## 6. Exportando Resultados

### Exportar como PDF

1. Na página de resultado, clique em **"Exportar PDF"**
2. O sistema gerará um relatório profissional com a imagem, resultados e metadados
3. O download iniciará automaticamente em alguns segundos

### Exportar como CSV

1. Clique em **"Exportar CSV"** para baixar os dados numéricos em formato de planilha
2. Compatível com Excel, R e Python pandas

---

## 7. Histórico de Análises

Acesse todas as análises anteriores em **"Histórico"** no menu lateral. Você pode:

- Filtrar por **data**, **tipo de análise** e **status**
- Clicar em qualquer análise para ver o resultado completo
- **Re-executar** análises anteriores com os mesmos parâmetros

---

## 8. Dicas e Boas Práticas para Melhores Resultados

| Aspecto | Recomendação |
|---|---|
| Qualidade de imagem | Use fotos com boa iluminação natural ou artificial uniforme, sem sombras fortes |
| Resolução | Mínimo **1280×960 pixels** para análises morfométricas precisas |
| Fundo | Fundos neutros (branco, cinza) melhoram a segmentação da planta |
| Escala | Inclua uma régua ou objeto de referência para calibração de dimensões |
| Modelo | GPT-4o ou Claude 3.5 Sonnet para maior precisão; GPT-4o-mini para triagem rápida |
| Tipo de planta | Informar o tipo melhora significativamente o diagnóstico de doenças e pragas |

---

## 9. Solução de Problemas Comuns

| Problema | Possível Causa | Solução |
|---|---|---|
| "Credencial Inválida" | Chave de API expirada ou incorreta | Vá em Configurações > Credenciais e re-valide ou recadastre a chave |
| Análise lenta (> 60s) | Alta demanda no provedor de IA | Tente um modelo mais rápido (ex: gpt-4o-mini) ou aguarde |
| "Erro ao processar imagem" | Formato não suportado ou arquivo corrompido | Converta a imagem para JPEG/PNG e tente novamente |
| Resultado com baixa confiança | Imagem com baixa resolução ou iluminação ruim | Envie uma nova imagem com melhor qualidade |
| Upload não avança | Imagem maior que 20 MB | Reduza o tamanho da imagem com ferramenta de compressão |

---

## 10. Suporte e Contato

- 📚 **Documentação online:** https://docs.phenoai.studio
- 📧 **Suporte por email:** suporte@phenoai.studio
- 💬 **Comunidade de pesquisadores:** https://community.phenoai.studio
- 🐛 **Relatar um bug ou sugestão:** https://github.com/org/phenoai-studio/issues
