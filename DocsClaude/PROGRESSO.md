# Progresso dos estudos

> **Fonte da verdade do plano de estudos.** Toda sessão de estudo lê este arquivo no começo e o atualiza no fim.
> Última atualização: **27/11/2026**

---

## 📍 Onde estou agora

| | |
|---|---|
| **Fase** | 2: Spring Boot & APIs REST (dias 21–45) |
| **Projeto** | Finance-Api (API REST de finanças pessoais) |
| **Estudo em andamento** | nenhum |
| **Próximo estudo** | o primeiro item não marcado da [fila](#-fila-de-estudos) |
| **Branch atual** | `Testes-automatizados-valid` (com os testes de validação ainda sem commit) |
| **Pasta de trabalho** | `/Users/nicolascouto/Documents/TreguWorkspace/Finance-Api` (a principal, sem worktree) |

---

## 🧭 Fila de estudos

A sessão central sempre escolhe o **primeiro item não marcado**. Cada item vira uma sessão de estudo, uma branch e um guia `DocsClaude/DIA_XX_TEMA.md`.

- [ ] **1. Fechar o dia 24:** passo 8 do `DIA_24_VALIDACAO.md` (teste manual com curl, com MySQL e JWT), commitar os testes de validação e abrir o PR da `Testes-automatizados-valid` para a `master`
- [ ] **2. Juntar os 186 testes da `feat/integration-test`:** merge na branch atual, resolver conflitos (os pacotes mudaram no "Organinzing packeds" e o `isValid` agora recebe `TokenType`) e trazer de volta o `contextLoads()` usando H2. É um estudo de git: merge e conflitos.
- [ ] **3. Dia 22 (pendência): DTOs de resposta:** `AccountResponse` e `TransactionResponse` no lugar das entidades JPA (item 3.1.7 do `TECNOLOGIAS_E_PROXIMOS_PASSOS.md`)
- [ ] **4. Correções rápidas:** `ddl-auto: validate`, dependências duplicadas no Gradle, e-mail duplicado no cadastro → 409 (itens 3.1.3, 3.1.8 e 3.1.9)
- [ ] **5. Dia 28: Paginação e filtros:** `Pageable` + `Specification` em `/transactions` e `/accounts`
- [ ] **6. Dia 33: Roles e permissões:** dono da conta (migration V5), `@PreAuthorize` e saldo que só muda por transação (item 3.1.4)
- [ ] **7. Cache e concorrência:** `@CacheEvict` nas transferências e `@Version` (itens 3.1.5 e 3.1.6)
- [ ] **8. Dia 34: Testcontainers:** testes de integração num MySQL de verdade
- [ ] **9. Dia 36: OpenAPI / Swagger**
- [ ] **10. Dia 37: Logging estruturado:** MDC e correlation ID
- [ ] **11. Dia 39: Actuator:** health e métricas
- [ ] **12. Dia 41: ProblemDetail (RFC 9457)** no lugar do `ApiError`
- [ ] **13. Dia 42: Idempotência** no `POST /transactions`
- [ ] **14. Dia 43: Rate limiting** com Bucket4j
- [ ] **15. Dia 29: Cache com Redis** no lugar do Caffeine
- [ ] **16. Dia 38: Profiles e segredos fora do código** (adiado de propósito até aqui)
- [ ] **17. Dia 40: README**
- [ ] **18. Dia 44: Revisão da API**
- [ ] **19. Dia 45: Deploy** (Railway ou Render) → fim da Fase 2

Depois disso começa a **Fase 3** (dias 46–65, banco de dados avançado) e a fila é refeita com o projeto novo.

---

## 🗺️ Roadmap da Fase 2

✅ feito · 🟡 parcial · ⬜ não começado

| Dia | Tema | Status | Observação |
|---|---|---|---|
| 21 | Setup do Spring Boot | ✅ | |
| 22 | Controllers REST e DTOs | 🟡 | Falta DTO de resposta (fila, item 3) |
| 23 | HTTP na prática | ✅ | |
| 24 | Validação | 🟡 | Passos 1–7 feitos; falta o passo 8 (fila, item 1) |
| 25 | Projeto: primeiros endpoints | ✅ | |
| 26 | Spring Data JPA | ✅ | |
| 27 | Migrations (Flyway) | ✅ | V1 a V4 |
| 28 | Paginação e filtros | ⬜ | |
| 29 | Cache com Redis | 🟡 | Hoje usa Caffeine |
| 30 | Projeto: persistir com JPA | ✅ | |
| 31 | Spring Security | ✅ | |
| 32 | JWT e refresh token | ✅ | Tipo do token (`TokenType`) corrigido em 16/11 |
| 33 | Roles e permissões | 🟡 | Roles existem, mas nenhum `@PreAuthorize` |
| 34 | Testes de integração | 🟡 | MockMvc + H2 na `feat/integration-test`; Testcontainers não |
| 35 | Projeto: JWT na API | ✅ | |
| 36 | OpenAPI / Swagger | ⬜ | |
| 37 | Logging estruturado | ⬜ | |
| 38 | Profiles e configs | ⬜ | Segredos no `application.yaml` (adiado até o deploy) |
| 39 | Actuator | ⬜ | |
| 40 | Projeto: Swagger + README | ⬜ | |
| 41 | ProblemDetail | ⬜ | |
| 42 | Idempotência | ⬜ | |
| 43 | Rate limiting | ⬜ | |
| 44 | Revisão da API | ⬜ | |
| 45 | Deploy | ⬜ | |

**Próximas fases** (o plano completo está na mensagem original do roadmap da Tregu):
- **Fase 3, dias 46–65:** banco de dados avançado (SQL avançado, índices, transações, PostgreSQL, MongoDB, Redis, otimização, modelagem). Projeto: sistema de relatórios analíticos.
- **Fase 4, dias 66–85:** IA e LLMs no backend (Spring AI, prompts, embeddings, pgvector, RAG, tool calling). Projeto: API de assistente com RAG.

---

## ⚠️ Pendências e avisos

- **Testes de validação sem commit** na `Testes-automatizados-valid`: `AccountRequestTest` (7), `TransactionRequestTest` (11) e `AccountControllerValidationTest` (1).
- **`contextLoads()` removido** do `FinanceApiApplicationTests` porque exigia o MySQL ligado. Voltar com ele no item 2 da fila, usando H2.
- **Os 186 testes antigos** estão na branch `feat/integration-test` (local e no GitHub), com a estrutura de pacotes antiga.
- **Segredos no `application.yaml`:** adiado por decisão do aluno até o dia 38/45.
- **Para rodar a aplicação:** MySQL no Docker (container `finance-api-mysql`, porta 3306) e a classe `FinanceApiApplicationKt` no IntelliJ.

---

## 🎓 Como estudamos (regras para as sessões de estudo)

- **O objetivo é o aluno aprender a codar.** Explicar o conceito, dar o passo a passo e revisar o código dele. Não escrever o código do projeto por ele.
  - **Exceção:** quando ele pedir explicitamente ("escreve você", "me mostre a correção", "ajeita aí"), escrever e depois explicar o que mudou.
- **Tarefas soltas não bastam.** Ele prefere ver **o que escrever, onde e por quê**, parte por parte, com o primeiro exemplo feito junto e os seguintes como exercício.
- **Começar pelo mais simples.** Subir de nível aos poucos (ex.: testes: DTO puro → service com MockK → controller com MockMvc).
- **Um passo por vez.** Ele diz "passo N" para avançar e "valide" para eu conferir.
- **"Valide" = conferir de verdade:** ler o arquivo, compilar, rodar os testes ou a API **numa cópia temporária** e responder com uma tabela de resultados. Nunca alterar o código dele ao validar.
- **Antes de entregar código**, testar numa cópia temporária.
- **Git:** ele roda os comandos (está aprendendo). Uma branch por estudo, a partir da `master` atualizada, e PR no fim.
- **Idioma:** português, linguagem simples, sem jargão desnecessário.

---

## 📒 Histórico

| Data | Estudo | Resultado |
|---|---|---|
| 13/11/2026 | Documento de tecnologias e próximos passos | `TECNOLOGIAS_E_PROXIMOS_PASSOS.md` |
| 16/11/2026 | Correção do tipo do token JWT | `TokenType` (ACCESS / REFRESH) |
| 16–27/11/2026 | Dia 24: Validação (passos 1–7) | Bean Validation nos DTOs, handler com lista de `fields`, `/error` liberado no Security, 19 testes de validação. PR #2 mergeado. |
| 26/11/2026 | Git | Remote trocado para SSH; projeto migrado do worktree para a pasta principal |
