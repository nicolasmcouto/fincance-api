# Finance API: tecnologias e próximos passos

API REST de contas e transferências, com autenticação JWT. É o projeto da **Fase 2 (Spring Boot & APIs REST)** do roadmap de estudos.

> **Situação em 13/11/2026:** branch `feat/integration-test`, **186 testes passando** (`./gradlew test`), ainda sem README e sem deploy.

---

## 1. Tecnologias usadas

| Tecnologia | Versão | Para que serve aqui | Onde olhar |
|---|---|---|---|
| **Kotlin** | 2.3.21 | Linguagem do projeto | `src/main/kotlin` |
| **Java (toolchain)** | 21 | JVM em que o projeto compila e roda | `build.gradle.kts` |
| **Gradle (Kotlin DSL)** | wrapper | Build, dependências e tarefas (`test`, `bootRun`) | `build.gradle.kts`, `settings.gradle.kts` |
| **Spring Boot** | 4.1.0 | Base da aplicação: injeção de dependência, configuração automática e servidor embutido | `FinanceApiApplication.kt` |
| **Spring Web MVC** | (Boot) | Controllers REST, rotas e códigos HTTP | `Controller/` |
| **Spring Data JPA + Hibernate** | (Boot) | Mapeia as entidades para tabelas e gera os repositórios | `Domain/Entity/`, `Domain/Repository/` |
| **MySQL** | conector `mysql-connector-j` | Banco de dados da aplicação | `application.yaml` |
| **Flyway** | (Boot) + `flyway-mysql` | Versiona o schema do banco com migrations numeradas | `resources/db/migration/V1..V4` |
| **Spring Cache + Caffeine** | (Boot) | Cache em memória de `findById` (contas e transações), com expiração de 10 min | `Service/`, `application.yaml` |
| **Spring Security** | (Boot) | Exige autenticação em todas as rotas, menos `/auth/**` | `config/SecurityConfig.kt` |
| **JJWT** | 0.12.6 | Gera e valida tokens JWT (access de 15 min, refresh de 7 dias) | `Security/JwtService.kt` |
| **BCrypt** | (Security) | Guarda o hash da senha, nunca a senha em si | `config/DefiniteUsers.kt` |
| **Jackson 3 + módulo Kotlin** | `tools.jackson` | Converte JSON ⇄ data classes Kotlin | automático |
| **Plugins `kotlin-spring` / `kotlin-jpa`** | 2.3.21 | Abrem as classes para os proxies do Spring e criam o construtor vazio exigido pelo JPA | `build.gradle.kts` |
| **JUnit 5** | (Boot) | Framework de testes | `src/test` |
| **MockK** | 1.14.11 | Mocks em Kotlin nos testes unitários | `Service/*Test.kt` |
| **MockMvc + spring-security-test** | (Boot) | Testa os controllers e a segurança sem subir um servidor de verdade | `Controller/*Test.kt`, `IntegrationTest/` |
| **H2** | (Boot) | Banco em memória usado nos testes | `application-test.yaml`, `IntegrationTest.kt` |

### Arquitetura

```
HTTP ──► JwtAuthFilter ──► Controller ──► Service ──► Repository ──► MySQL
          (lê o Bearer      (rotas,        (regras de    (Spring Data      ▲
           token e marca     DTO → entidade) negócio,     JPA)              │
           o usuário)                        @Transactional,           Flyway cria
                                             @Cacheable)               as tabelas
                     erros ──► GlobalExceptionHandler ──► JSON (404 / 422)
```

### Endpoints

| Método | Rota | Acesso | O que faz |
|---|---|---|---|
| POST | `/auth/registrar` | público | Cria um usuário com a role `USER` |
| POST | `/auth/login` | público | Devolve o access token e o refresh token |
| POST | `/auth/refresh` | público | Gera um novo access token a partir do refresh token |
| GET / POST | `/accounts` | autenticado | Lista ou cria contas |
| GET / PUT / DELETE | `/accounts/{id}` | autenticado | Busca, atualiza ou remove uma conta |
| GET / POST | `/transactions` | autenticado | Lista ou cria transferências (valida saldo e se as contas são diferentes) |
| GET / PUT / DELETE | `/transactions/{id}` | autenticado | Busca, atualiza ou remove uma transferência, estornando os saldos |

---

## 2. Onde você está no roadmap (Fase 2, dias 21–45)

✅ feito · 🟡 parcial · ⬜ não começado

| Dia | Tema | Status | Observação |
|---|---|---|---|
| 21 | Setup do Spring Boot | ✅ | |
| 22 | Controllers REST e DTOs | 🟡 | Tem DTO de entrada, mas a resposta devolve a entidade JPA |
| 23 | HTTP na prática | ✅ | Usa 201, 204, 404 e 422 |
| 24 | Validação | 🟡 | Tem o handler de exceções, mas não tem Bean Validation (`@Valid`) |
| 25 | Projeto: primeiros endpoints | ✅ | |
| 26 | Spring Data JPA | ✅ | Com relacionamentos `@ManyToOne` |
| 27 | Migrations (Flyway) | ✅ | V1 a V4 |
| 28 | Paginação e filtros | ⬜ | `findAll` devolve tudo de uma vez |
| 29 | Cache com Redis | 🟡 | O cache funciona, mas com Caffeine (local), não Redis |
| 30 | Projeto: persistir com JPA | ✅ | |
| 31 | Spring Security | ✅ | |
| 32 | JWT e refresh token | ✅ | O refresh token não é renovado a cada uso |
| 33 | Roles e permissões | 🟡 | As roles existem e `@EnableMethodSecurity` está ligado, mas nenhum `@PreAuthorize` é usado |
| 34 | Testes de integração | 🟡 | `@SpringBootTest` + MockMvc com H2; Testcontainers ainda não |
| 35 | Projeto: JWT na API | ✅ | |
| 36 | OpenAPI / Swagger | ⬜ | |
| 37 | Logging estruturado | ⬜ | |
| 38 | Profiles e configs | ⬜ | Os segredos estão escritos no `application.yaml` (ver 3.1) |
| 39 | Actuator | ⬜ | `/actuator/health` está liberado no Security, mas a dependência do Actuator não foi adicionada |
| 40 | Projeto: Swagger + README | ⬜ | Não existe README |
| 41 | ProblemDetail (RFC 9457) | ⬜ | Usa uma classe própria, `ApiError` |
| 42 | Idempotência | ⬜ | |
| 43 | Rate limiting | ⬜ | |
| 44 | Revisão da API | ⬜ | |
| 45 | Deploy | ⬜ | |

---

## 3. Próximos passos

### 3.1 Correções: faça antes de seguir o roadmap

1. **Segredos no repositório.** A senha do MySQL e o segredo do JWT estão escritos em `application.yaml` e já foram para o histórico do git.
   - Troque por variáveis de ambiente: `password: ${DB_PASSWORD}` e `secret: ${JWT_SECRET}`.
   - **Troque a senha do MySQL.** Apagar do arquivo não a remove do histórico, que fica visível para quem tiver acesso ao repositório no GitHub.
   - Isso já adianta o dia 38 (profiles e secrets).

2. **O refresh token funciona como access token.** Os dois tokens são assinados com a mesma chave e o `JwtAuthFilter` só confere a assinatura e a validade. Resultado: um refresh token, que vale 7 dias, é aceito em qualquer rota protegida.
   - Correção: adicionar um claim `type` (`"access"` / `"refresh"`) em `JwtService`. O filtro aceita só `access`, e o `/auth/refresh` aceita só `refresh`.
   - Escreva primeiro um teste que reproduza o problema e depois corrija.

3. **`ddl-auto: update` junto com o Flyway.** Os dois alteram o schema, e o Hibernate pode criar colunas que nenhuma migration registra. Troque para `ddl-auto: validate`: o Flyway cria as tabelas e o Hibernate só confere se elas batem com as entidades.

4. **O saldo pode ser editado direto.** O `PUT /accounts/{id}` aceita `balance`, então qualquer usuário autenticado pode mudar um saldo sem fazer transferência. O saldo deveria mudar só por transações: tire `balance` do DTO de atualização e deixe-o apenas na criação, como saldo inicial.

5. **Cache de entidades JPA mutáveis.** O `@Cacheable` guarda o *próprio objeto* `AccountEntity`. O `TransactionService` altera o saldo desse objeto e não invalida o cache `accounts`. Se a transação der rollback (por exemplo, um `PUT /transactions` que falha por saldo insuficiente depois do estorno), o banco volta ao estado anterior, mas o objeto em cache continua com o saldo alterado. Duas saídas:
   - guardar DTOs imutáveis no cache, e não entidades; **ou**
   - usar `@CacheEvict(value = ["accounts"], allEntries = true)` (ou por id) nas operações do `TransactionService`.

6. **Transferências simultâneas.** O `transfer()` lê o saldo, calcula e salva. Duas transferências ao mesmo tempo na mesma conta podem perder uma das atualizações (*lost update*). Adicione `@Version` em `AccountEntity` (lock otimista) ou `@Lock(PESSIMISTIC_WRITE)` na busca da conta. É uma prévia do dia 48 (transações e isolamento).

7. **Os controllers devolvem entidades.** Crie `AccountResponse` e `TransactionResponse`. Assim você não expõe a estrutura interna, evita erros de serialização com relacionamentos `LAZY` e consegue mudar o banco sem quebrar quem usa a API.

8. **Dependências duplicadas** no `build.gradle.kts`:
   - `testcontainers:junit-jupiter` aparece duas vezes, com versões diferentes (1.20.1 e 1.19.0);
   - `spring-boot-starter-web` e `spring-boot-starter-webmvc` estão juntos, e basta um.

9. **E-mail duplicado no cadastro** gera erro 500, por causa da constraint `uk_users_email`. Verifique antes de salvar e responda **409 Conflict**.

### 3.2 Continuação do roadmap aplicada a esta API

| Dia | O que implementar aqui |
|---|---|
| **24** Validação | `@Valid` nos controllers; `@NotBlank` em `ownerName`/`description`, `@Positive` em `amount`, `@Email` no cadastro; tratar `MethodArgumentNotValidException` → **400** |
| **28** Paginação | `GET /transactions?accountId=&type=&from=&to=&page=0&size=20` com `Pageable` + `Specification`; o mesmo para `/accounts` |
| **29** Redis | Trocar o Caffeine por Redis (subir com `docker compose`). Pensar: por que um cache local dá dados inconsistentes com 2 instâncias da API? |
| **33** RBAC | Ligar a conta ao usuário: migration `V5` com `owner_id` → `users.id`. `USER` vê e movimenta só as próprias contas; `ADMIN` vê tudo, com `@PreAuthorize("hasRole('ADMIN')")` no `findAll` |
| **34** Testcontainers | Rodar os testes de integração num MySQL de verdade. Também resolve a mistura de modos do H2 (`application-test.yaml` usa `MODE=PostgreSQL` e o `IntegrationTest` usa `MODE=MySQL`) |
| **36** OpenAPI | `springdoc-openapi` (versão compatível com Spring Boot 4), Swagger UI em `/swagger-ui.html`, com esquema `bearerAuth` para testar com token |
| **37** Logging | Filtro que gera um `X-Correlation-Id` e coloca no MDC; log estruturado em cada transferência (origem, destino, valor) |
| **38** Profiles | `application-dev.yaml` e `application-prod.yaml`; segredos só por variável de ambiente (continuação do item 3.1.1) |
| **39** Actuator | Adicionar `spring-boot-starter-actuator`; expor `health` e `metrics` |
| **40** README | Como rodar (MySQL + variáveis), endpoints, exemplo de login com `curl` e link para o Swagger |
| **41** ProblemDetail | Trocar o `ApiError` por `ProblemDetail` (RFC 9457); adicionar handler genérico para 500 e para JSON malformado (400) |
| **42** Idempotência | Header `Idempotency-Key` no `POST /transactions`: a mesma chave devolve a mesma resposta e não transfere duas vezes. Criar uma tabela `idempotency_keys` via migration |
| **43** Rate limiting | Bucket4j no `/auth/login` (contra força bruta) e no `POST /transactions` |
| **44** Revisão | Usar este documento como checklist |
| **45** Deploy | `Dockerfile` + Railway/Render com MySQL gerenciado, variáveis de ambiente configuradas e o link no README |

### 3.3 Melhorias menores (quando sobrar tempo)

- **Nomes de pacotes em minúsculo**, que é a convenção do Kotlin: `controller`, `domain`, `exception`, `security`, `service`.
- **Erros de digitação:** `acess` → `access` (`JwtService`, `AuthController`), `studing` → `studying` (pacote e `group`), `DefiniteUsers` → `PasswordEncoderConfig`.
- **`TransactionType` (`INCOME` / `EXPENSE`)** não combina com o modelo de transferência entre duas contas. Decida se a transação é uma *transferência* (origem → destino) ou uma *receita/despesa* de uma conta só, e ajuste o modelo.
- **Ordem das migrations:** a `V3` referencia `users` antes de a `V4` criar a tabela. Só funcionou porque o InnoDB ignora `REFERENCES` declarado na linha da coluna. Não edite migrations que já rodaram: corrija sempre com uma migration nova, como a V4 fez.

---

## 4. Ordem sugerida

1. Segredos fora do repositório + profiles (3.1.1, dia 38)
2. Tipo do token JWT (3.1.2)
3. `ddl-auto: validate`, dependências duplicadas, e-mail duplicado (3.1.3, 3.1.8, 3.1.9)
4. DTOs de resposta + Bean Validation + ProblemDetail (3.1.7, dias 24 e 41)
5. Saldo só por transação + dono da conta + `@PreAuthorize` (3.1.4, dia 33)
6. Cache e concorrência nas transferências (3.1.5, 3.1.6)
7. Paginação e filtros (dia 28)
8. Testcontainers (dia 34)
9. Swagger, Actuator e logging (dias 36, 37, 39)
10. Idempotência e rate limiting (dias 42, 43)
11. Redis (dia 29)
12. README + deploy (dias 40, 45)
