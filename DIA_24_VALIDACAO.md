# Dia 24: validação de entrada (Bean Validation + exception handlers)

> **Fase 2, semana 1.** Antes: dia 23 (HTTP na prática). Depois: dia 25 (projeto) e dia 26 (Spring Data JPA).
> **Tempo:** 1h de leitura e conceitos. A implementação pode passar para o dia seguinte.
> **Onde:** nesta cópia do projeto (`finance-api-startup-cd740c`).

---

## 0. O objetivo do dia em uma tabela

Ao final, a sua API vai responder assim:

| Requisição | Hoje | Depois do dia 24 |
|---|---|---|
| Criar conta com `ownerName: ""` | ✅ 201, conta criada sem nome | ❌ **400** dizendo qual campo está errado |
| Transferência com `amount: -100` | ✅ 201 e **o dinheiro anda ao contrário** (ver seção 1) | ❌ **400** |
| Cadastro com `email: "abc"` | ✅ 201 | ❌ **400** |
| JSON sem o campo `ownerName` | 400 com uma resposta padrão do Spring, diferente do seu `ApiError` | ❌ **400** no formato do seu `ApiError` |
| JSON quebrado (faltando `}`) | 400 com resposta padrão | ❌ **400** no formato do seu `ApiError` |
| Transferência com saldo insuficiente | 422 | 422 (não muda: é regra de negócio) |

---

## 1. Por que validar? Um furo real no seu código

Regra número 1 de backend: **nunca confie no que o cliente manda.** O frontend pode validar à vontade, mas qualquer pessoa consegue chamar a sua API direto, com `curl` ou Postman, e mandar o que quiser.

Veja o que acontece hoje com uma transferência de **valor negativo** no `TransactionService`:

```kotlin
// validateTransfer
if (transaction.sender.balance < transaction.amount)   // 50 < -100 ? NÃO → passa
    throw InsufficientBalanceException(...)

// transfer
sender.balance   = sender.balance.subtract(amount)     // 50 - (-100) = 150  ← ganhou dinheiro
receiver.balance = receiver.balance.subtract(...)       // receiver PERDE 100
```

Ou seja: **qualquer usuário autenticado consegue puxar dinheiro de qualquer conta** mandando um valor negativo. Validação de entrada não é frescura, é segurança.

---

## 2. As três camadas de defesa

Uma requisição passa por três "peneiras" antes de virar dado no banco:

```
JSON chega
   │
   ▼
① Jackson (desserialização)       "Esse JSON tem o FORMATO certo?"
   │  JSON quebrado, campo obrigatório ausente,
   │  enum inexistente ("type": "XPTO"), texto onde devia ser número
   │  ✖ falhou → HttpMessageNotReadableException → 400
   ▼
② Bean Validation (@Valid)         "Os VALORES fazem sentido?"
   │  nome em branco, valor negativo, e-mail inválido, texto longo demais
   │  ✖ falhou → MethodArgumentNotValidException → 400
   ▼
③ Service (regras de negócio)      "A OPERAÇÃO é permitida AGORA?"
   │  saldo insuficiente, transferir para a mesma conta
   │  ✖ falhou → suas exceções → 422
   ▼
Banco de dados
```

### 400 ou 422?
- **400 Bad Request:** *"o que você mandou está mal formado"*. O cliente errou o formato e nem adianta tentar de novo sem corrigir.
- **422 Unprocessable Entity:** *"entendi o pedido, mas não posso fazer isso"*. O formato está certo, e o que impede é o estado do sistema (por exemplo, o saldo). A mesma requisição pode funcionar amanhã, se a conta receber dinheiro.

> 🧠 **Regra de bolso:** se dá para validar **olhando só o JSON**, sem consultar o banco, é camada ②. Se precisa consultar o banco, é camada ③.

---

## 3. Conceitos

### 3.1 O que é Bean Validation
- **Jakarta Bean Validation** é uma *especificação*: um conjunto de anotações como `@NotBlank` e `@Email`, mais as regras de como elas funcionam.
- **Hibernate Validator** é a *implementação* que faz o trabalho de verdade (não tem relação com o Hibernate do JPA, é só o mesmo grupo de desenvolvedores).
- **`spring-boot-starter-validation`** traz os dois e liga tudo no Spring.

### 3.2 Como funciona no Spring
1. Você anota os campos do DTO: `@NotBlank`, `@Positive`...
2. Você coloca `@Valid` no parâmetro do controller: `fun create(@Valid @RequestBody request: AccountRequest)`.
3. Antes de entrar no seu método, o Spring valida o objeto. Se algo falhar, ele **nem chama o seu método** e lança `MethodArgumentNotValidException`.
4. O seu `@RestControllerAdvice` captura essa exceção e monta a resposta.

> ⚠️ **Sem o `@Valid`, as anotações são ignoradas em silêncio.** É o erro mais comum de todos.

### 3.3 As anotações que você vai usar

| Anotação | Valida que... | Exemplo de uso |
|---|---|---|
| `@NotBlank` | o texto não é vazio nem só espaços | nomes, descrições |
| `@NotEmpty` | o texto ou a lista não é vazio (mas `"   "` passa) | listas |
| `@NotNull` | não é `null` | tipos *nullable* (ver 4.2) |
| `@Size(min, max)` | o tamanho do texto ou da lista está entre min e max | `@Size(max = 255)` |
| `@Email` | parece um e-mail | cadastro |
| `@Positive` / `@PositiveOrZero` | o número é > 0 / ≥ 0 | valores, ids |
| `@DecimalMin("0.01")` | o número é ≥ 0.01 | valores monetários |
| `@Digits(integer, fraction)` | limita as casas antes e depois da vírgula | dinheiro: `@Digits(integer = 17, fraction = 2)` |
| `@PastOrPresent` / `@Future` | a data é no passado ou presente / no futuro | datas |
| `@Pattern(regexp)` | o texto bate com uma regex | CPF, telefone |

**A diferença que mais cai em entrevista:**

| Valor | `@NotNull` | `@NotEmpty` | `@NotBlank` |
|---|---|---|---|
| `null` | ❌ | ❌ | ❌ |
| `""` | ✅ | ❌ | ❌ |
| `"   "` | ✅ | ✅ | ❌ |
| `"Ana"` | ✅ | ✅ | ✅ |

Toda anotação aceita uma mensagem própria: `@NotBlank(message = "O nome é obrigatório")`.

**Exemplo genérico**, de um DTO que *não* é do seu projeto, só para mostrar a sintaxe:

```kotlin
data class ProdutoRequest(
    @field:NotBlank(message = "O nome é obrigatório")
    @field:Size(max = 100, message = "O nome pode ter no máximo 100 caracteres")
    val nome: String,

    @field:Positive(message = "O preço precisa ser maior que zero")
    val preco: BigDecimal,
)
```

---

## 4. Pegadinhas do Kotlin (leia com calma)

### 4.1 Onde a anotação "gruda": `@field:`
Em Kotlin, `val nome: String` dentro do construtor é ao mesmo tempo **parâmetro do construtor**, **propriedade** e **campo** (field). Quando você escreve `@NotBlank val nome`, em qual dos três a anotação vai parar?

- **Antigamente**, ela ia só para o *parâmetro*. O Hibernate Validator olha o *campo*, então a validação era **ignorada em silêncio**. A solução era escrever `@field:NotBlank`.
- **No seu projeto**, o `build.gradle.kts` tem o flag `-Xannotation-default-target=param-property`, que faz a anotação ir para o parâmetro **e** para o campo. Por isso `@NotBlank` sozinho funciona. Eu testei antes de escrever este guia.

> **Recomendação:** use `@field:NotBlank` mesmo assim. Deixa explícito o que está acontecendo e funciona em qualquer projeto Kotlin, com ou sem o flag.

### 4.2 Tipos não-nulos e campo ausente
O seu DTO tem `val ownerName: String` (não-nulo, sem valor padrão). Se o JSON **não tiver** o campo `ownerName`, o que acontece?

O **Jackson** (camada ①) nem consegue criar o objeto, porque o Kotlin não aceita `null` ali. Ele lança `HttpMessageNotReadableException` **antes** da validação rodar.

Consequências:
- `@NotNull` num tipo não-nulo do Kotlin é **inútil**: o Jackson barra antes.
- O campo ausente não aparece na lista de erros da validação. Ele vira outro tipo de erro, que você vai tratar no passo 6.

**Alternativa** (só para conhecer): declarar os campos como `String?` com `@field:NotNull`. Assim *todos* os erros aparecem juntos numa resposta só, mas você passa a precisar de `!!` ou `?:` no resto do código. Para este projeto, **mantenha os tipos não-nulos**.

### 4.3 Objetos aninhados
Se um DTO tivesse outro DTO dentro (por exemplo, `val endereco: EnderecoRequest`), seria preciso `@field:Valid` nessa propriedade para a validação "descer" até ele. Os seus DTOs não têm isso hoje, mas guarde a informação.

---

## 5. Mão na massa

> Faça um passo de cada vez e rode os testes ao final de cada um. Quando travar, abra as dicas na ordem: elas vão da mais vaga para a mais direta.

### Passo 1: adicionar a dependência
No `build.gradle.kts`, junto das outras `implementation`:

```kotlin
implementation("org.springframework.boot:spring-boot-starter-validation")
```

Depois, **recarregue o Gradle** (⌘ + ⇧ + I, ou ⌘ + ⇧ + A → "Reload All Gradle Projects").

- [ ] Dependência adicionada e Gradle recarregado
- [ ] `import jakarta.validation.constraints.NotBlank` é reconhecido no IntelliJ

---

### Passo 2: regras do `AccountRequest`

| Campo | Regra | Por quê |
|---|---|---|
| `ownerName` | obrigatório, não pode ser só espaços, no máximo 255 caracteres | A coluna no banco é `VARCHAR(255)`. Sem o limite, um nome de 300 letras quebra no banco com erro 500 |
| `type` | (nada) | O Jackson já barra valores inválidos, porque é um enum não-nulo |
| `balance` | maior ou igual a zero, no máximo 2 casas decimais e 17 dígitos inteiros | A coluna é `DECIMAL(19,2)`. Conta não começa negativa |

- [ ] Anotações adicionadas no `AccountRequest`

<details><summary>💡 Dica 1</summary>

Olhe a tabela da seção 3.3. São três anotações em `ownerName` (contando o `@Size`) e duas em `balance`.
</details>

<details><summary>💡 Dica 2</summary>

`ownerName`: `@NotBlank` + `@Size(max = 255)`. `balance`: `@PositiveOrZero` + `@Digits(integer = 17, fraction = 2)`. Não esqueça do `@field:` e de colocar uma `message` em português.
</details>

---

### Passo 3: colocar `@Valid` nos controllers

Adicione `@Valid` antes de `@RequestBody` em **todos** os métodos que recebem corpo:

- [ ] `AccountController`: `create` e `update`
- [ ] `TransactionController`: `create` e `update`
- [ ] `AuthController`: `login` e `registrar` (o `refresh` também pode, depois do passo 5)

O import é `jakarta.validation.Valid`.

- [ ] `SecurityConfig`: liberar a rota `/error` com `authorize("/error", permitAll)`, junto do `/auth/**`

> 🕵️ **Por que liberar o `/error`?** Quando a validação falha, o Spring "encaminha" a requisição internamente para a rota `/error`, que monta a resposta de erro. Nesse encaminhamento o `JwtAuthFilter` não roda de novo, então o usuário chega lá **sem estar autenticado**. A regra `anyRequest, authenticated` barra, e o cliente recebe um **403** confuso em vez do 400. Depois do passo 6 o seu handler responde direto, sem passar pelo `/error`, mas liberar a rota evita esse 403 em qualquer outro erro.

> 🧪 **Teste rápido:** rode a aplicação e crie uma conta com `"ownerName": ""`. Deve voltar **400**, ainda com uma resposta feia. É o passo 6 que deixa ela bonita.

---

### Passo 4: regras do `TransactionRequest`

| Campo | Regra | Pense |
|---|---|---|
| `description` | obrigatória, sem ser só espaços, no máximo 255 caracteres | Mesmo motivo do `ownerName` |
| `amount` | **maior que zero**, no máximo 2 casas decimais | Isso fecha o furo da seção 1! Por que `@Positive` e não `@PositiveOrZero`? |
| `date` | ? | **Decisão sua:** faz sentido aceitar transferência com data no futuro? E muito no passado? |
| `senderId` / `receiverId` | maior que zero | Ids no banco começam em 1 |

> ❓ E "remetente diferente do destinatário"? Isso também dá para validar olhando só o JSON... Mas hoje o `TransactionService` já trata e devolve 422. Por enquanto **deixe como está**. Existe um desafio extra sobre isso na seção 7.

- [ ] Anotações adicionadas no `TransactionRequest`
- [ ] Decisão sobre `date` tomada (escreva num comentário o porquê)

---

### Passo 5: regras de cadastro e login

**`RegisterRequest`:**

| Campo | Regra |
|---|---|
| `email` | obrigatório e com formato de e-mail |
| `password` | obrigatória, **entre 8 e 72 caracteres** |

> 🤓 **Por que no máximo 72?** O BCrypt, que você usa para gerar o hash da senha, **ignora tudo depois do 72º byte**. Duas senhas diferentes que começam com os mesmos 72 caracteres teriam o mesmo hash.

**`LoginRequest`** (fica dentro do `AuthController.kt`):

| Campo | Regra |
|---|---|
| `email` | obrigatório |
| `senha` | obrigatória |

> ❓ **Pense:** por que **não** colocar `@Size(min = 8)` e `@Email` no login? (Dica: o que acontece com um usuário antigo cadastrado antes da regra existir? E o que um atacante aprende se o login responder "a senha precisa ter 8 caracteres"?)

- [ ] `RegisterRequest` validado
- [ ] `LoginRequest` validado

---

### Passo 6: o handler, uma resposta bonita e útil

Hoje o `GlobalExceptionHandler` devolve um `ApiError` com `timestamp`, `status`, `error` e `message`. Para erros de validação, o cliente precisa saber **qual campo** está errado e **por quê**.

**A resposta que você quer montar:**

```json
{
  "timestamp": "2026-11-16T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Dados inválidos",
  "fields": [
    { "field": "ownerName", "message": "O nome é obrigatório" },
    { "field": "balance",   "message": "O saldo não pode ser negativo" }
  ]
}
```

**Tarefas:**
- [ ] Criar uma data class para cada erro de campo (`field` + `message`)
- [ ] Adicionar ao `ApiError` uma lista opcional `fields`, com valor padrão `null`, para não quebrar os handlers que já existem
- [ ] Criar um `@ExceptionHandler(MethodArgumentNotValidException::class)` que devolve **400** com a lista
- [ ] Criar um `@ExceptionHandler(HttpMessageNotReadableException::class)` que devolve **400** com uma mensagem genérica, como `"JSON inválido ou campo obrigatório ausente"`

> 🔒 **Não devolva `ex.message` no handler do `HttpMessageNotReadableException`.** Ela contém detalhes internos (nomes de classes e pacotes do seu código), como: `"JSON parse error: Instantiation of [simple type, class tregu_studing_plan.finance_api.Domain.Dto.AccountRequest]..."`. Isso é vazamento de informação.

> 💡 **Opcional:** para o `@JsonInclude(JsonInclude.Include.NON_NULL)` esconder o `fields` quando ele for `null`, pesquise essa anotação.

<details><summary>💡 Dica 1: de onde tirar os erros?</summary>

A `MethodArgumentNotValidException` tem uma propriedade `bindingResult`. Explore no IntelliJ: digite `ex.bindingResult.` e veja o que o autocomplete oferece.
</details>

<details><summary>💡 Dica 2: quais propriedades usar?</summary>

`ex.bindingResult.fieldErrors` é uma lista de `FieldError`. Cada um tem `.field` (o nome do campo) e `.defaultMessage` (a mensagem que você escreveu na anotação, que pode ser `null`). Transforme essa lista com `.map { }`, conteúdo do dia 3 (coleções).
</details>

<details><summary>💡 Dica 3: o esqueleto</summary>

```kotlin
@ExceptionHandler(MethodArgumentNotValidException::class)
fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiError> {
    val fields = // ex.bindingResult.fieldErrors transformado em lista de erros de campo
    // reaproveite o build() que já existe? ele precisa aceitar a lista de campos...
}
```

O seu `build()` privado hoje recebe `status` e `message`. Uma saída é adicionar um terceiro parâmetro opcional.
</details>

---

### Passo 7: testes automatizados

Os testes vão rodar **sem banco e sem Spring Security**, usando o MockMvc em modo *standalone*: ele cria só o controller e o handler, o que deixa o teste rápido.

**7a.** Adicione o MockK, a biblioteca de mocks em Kotlin que a outra branch já usa:

```kotlin
testImplementation("io.mockk:mockk:1.14.11")
```

**7b.** Crie `src/test/kotlin/tregu_studing_plan/finance_api/Controller/AccountControllerValidationTest.kt`. Este é o **esqueleto de configuração** (não é a parte que ensina, por isso está pronto):

```kotlin
class AccountControllerValidationTest {

    private val accountService = mockk<AccountService>()

    private val mvc = MockMvcBuilders
        .standaloneSetup(AccountController(accountService))
        .setControllerAdvice(GlobalExceptionHandler())
        .build()

    private fun postAccount(json: String) =
        mvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(json))

    // escreva os seus testes aqui
}
```

**7c. Os testes que você escreve:**

- [ ] `ownerName` vazio → 400 e `$.fields[0].field` igual a `"ownerName"`
- [ ] `ownerName` só com espaços → 400
- [ ] `balance` negativo → 400
- [ ] dois campos errados ao mesmo tempo → 400 e `$.fields.length()` igual a 2
- [ ] campo `ownerName` ausente no JSON → 400 **sem** a lista `fields`
- [ ] `type` inválido (`"XPTO"`) → 400
- [ ] JSON quebrado → 400
- [ ] **o caso feliz:** JSON válido → 201. Este precisa de `every { accountService.create(any()) } returns ...`
- [ ] **O service nunca é chamado quando a validação falha:** `verify(exactly = 0) { accountService.create(any()) }`. Por que esse teste é importante?

Depois faça o mesmo para o `TransactionController` (principalmente o **`amount` negativo**!) e para o cadastro.

<details><summary>💡 Dica: como checar o JSON da resposta</summary>

```kotlin
postAccount("""{"ownerName":"","type":"CHECKING","balance":10}""")
    .andExpect(status().isBadRequest)
    .andExpect(jsonPath("$.fields[0].field").value("ownerName"))
```

As três aspas (`"""`) criam uma *raw string* em Kotlin, então não precisa escapar as aspas do JSON.
</details>

---

### Passo 8: teste manual com `curl`

Com o MySQL rodando e a aplicação no ar (`./gradlew bootRun` ou o ▶ do IntelliJ):

```bash
curl -s -X POST localhost:8080/auth/registrar -H "Content-Type: application/json" -d '{"email":"teste@exemplo.com","password":"senha-forte-123"}' -w "\nstatus %{http_code}\n"
```

```bash
curl -s -X POST localhost:8080/auth/login -H "Content-Type: application/json" -d '{"email":"teste@exemplo.com","senha":"senha-forte-123"}'
```

Copie o `accessToken` da resposta e use no lugar de `SEU_TOKEN`:

```bash
curl -s -X POST localhost:8080/accounts -H "Authorization: Bearer SEU_TOKEN" -H "Content-Type: application/json" -d '{"ownerName":"","type":"CHECKING","balance":-5}' -w "\nstatus %{http_code}\n"
```

- [ ] Volta 400 com os **dois** erros na lista `fields`
- [ ] Um cadastro com e-mail `"abc"` volta 400
- [ ] Uma transferência com `amount` negativo volta 400

---

## 6. Pronto quando

- [ ] Todos os DTOs de entrada têm validação
- [ ] Todos os `@RequestBody` têm `@Valid`
- [ ] Erro de validação → 400 com a lista de campos
- [ ] JSON inválido ou campo ausente → 400 no formato `ApiError`, sem vazar detalhes internos
- [ ] A transferência negativa está barrada (o furo da seção 1 fechado!)
- [ ] Testes automatizados cobrindo os casos do passo 7, e `./gradlew test` verde
- [ ] Commit com uma mensagem clara, por exemplo `feat: validação de entrada com Bean Validation`

---

## 7. Desafios extras (opcionais)

1. **Anotação própria `@ContasDiferentes`.** Crie uma *constraint customizada* no nível da classe, que valida `senderId != receiverId` no `TransactionRequest`. Pesquise `ConstraintValidator`. Depois discuta: ainda vale manter a checagem no service? (Dica: quem mais pode chamar o service sem passar pelo controller?)
2. **Validar `@PathVariable`.** Faça `GET /accounts/-1` voltar 400 em vez de 404. Pesquise `@Positive` em parâmetros de método e a `HandlerMethodValidationException`, do Spring 6.1+.
3. **Grupos de validação.** O `balance` só deveria ser aceito na **criação**, não na atualização (item 3.1.4 do `TECNOLOGIAS_E_PROXIMOS_PASSOS.md`). Resolva com dois DTOs (`CreateAccountRequest` e `UpdateAccountRequest`) ou com *validation groups*. Qual opção é mais simples de entender?
4. **Mensagens em arquivo.** Tire as mensagens das anotações e coloque num `ValidationMessages.properties`, com `message = "{conta.nome.obrigatorio}"`. Para que isso serve num sistema em vários idiomas?

---

## 8. Perguntas para fixar

1. O que acontece se você anotar o DTO mas esquecer o `@Valid` no controller?
2. Qual é a diferença entre `@NotNull`, `@NotEmpty` e `@NotBlank`?
3. Por que um campo ausente no JSON não aparece na lista `fields`?
4. Quando usar 400, e quando usar 422? Dê um exemplo de cada no seu projeto.
5. Se o frontend já valida tudo, por que validar de novo no backend?
6. Por que não devolver `ex.message` para o cliente?

---

## 9. Glossário rápido

| Termo | Significado |
|---|---|
| **DTO** | *Data Transfer Object*: a classe que representa o JSON que entra ou sai da API, separada da entidade do banco |
| **Desserialização** | Transformar texto (JSON) em objeto Kotlin. Quem faz é o Jackson |
| **Constraint** | Uma regra de validação, como `@NotBlank` |
| **`@RestControllerAdvice`** | Classe que captura exceções de todos os controllers num lugar só |
| **MockMvc** | Ferramenta para simular requisições HTTP nos testes sem subir um servidor |
| **Standalone** | Modo do MockMvc que cria só o controller indicado, sem carregar o Spring inteiro |
