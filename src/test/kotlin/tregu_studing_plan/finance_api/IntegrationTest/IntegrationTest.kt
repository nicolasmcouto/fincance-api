package tregu_studing_plan.finance_api.IntegrationTest

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import tregu_studing_plan.finance_api.Domain.Dto.TransactionRequest
import tregu_studing_plan.finance_api.Domain.Entity.AccountEntity
import tregu_studing_plan.finance_api.Domain.Entity.TransactionEntity
import tregu_studing_plan.finance_api.Domain.Enum.AccountType
import tregu_studing_plan.finance_api.Domain.Enum.TransactionType
import tregu_studing_plan.finance_api.Domain.Repository.AccountRepository
import tregu_studing_plan.finance_api.Domain.Repository.TransactionRepository
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals

/**
 * Testes de integracao ponta a ponta: MockMvc real + contexto Spring completo +
 * banco H2 em memoria (perfil "test", ver src/test/resources/application-test.yaml).
 *
 * `@Transactional` faz rollback automatico apos cada teste, entao cada metodo
 * comeca com o banco vazio e as contas criadas em `setup()` nunca vazam entre testes.
 *
 * O dominio real deste projeto e AccountEntity/TransactionEntity em /accounts e
 * /transactions — nao existe endpoint de saldo, filtro por tipo, nem validacao de
 * Bean Validation (valor negativo ou descricao vazia hoje NAO retornam 400; a
 * unica validacao de negocio e feita em TransactionService: saldo insuficiente e
 * transferencia para a mesma conta, ambos como 422).
 *
 * `SecurityConfig` exige autenticacao em qualquer rota fora do prefixo `/auth`, e
 * o filtro roda de verdade aqui (sem `addFilters = false`). `.with(user(...))`
 * injeta um principal ja autenticado no contexto de seguranca antes do request,
 * sem precisar emitir um JWT de verdade — o fluxo de login/token ja tem cobertura
 * dedicada em AuthControllerTest e JwtAuthFilterTest.
 */
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:integration;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ],
)
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class IntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var accountRepository: AccountRepository

    @Autowired
    private lateinit var transactionRepository: TransactionRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var joao: AccountEntity
    private lateinit var maria: AccountEntity

    @BeforeEach
    fun setup() {
        joao = accountRepository.save(AccountEntity(ownerName = "João", type = AccountType.CHECKING, balance = BigDecimal("5000.00")))
        maria = accountRepository.save(AccountEntity(ownerName = "Maria", type = AccountType.CHECKING, balance = BigDecimal("1000.00")))
    }

    private fun pedido(
        senderId: Long = joao.id!!,
        receiverId: Long = maria.id!!,
        amount: BigDecimal = BigDecimal("3000.00"),
        description: String = "teste",
        type: TransactionType = TransactionType.INCOME,
        date: LocalDateTime = LocalDateTime.of(2026, 1, 15, 10, 30),
    ) = TransactionRequest(description, amount, type, date, senderId, receiverId)

    @Nested
    inner class Criar {

        @Test
        fun `deve criar uma transacao e retornar 201`() {
            mockMvc.perform(
                post("/transactions")
                    .with(user("alice@tregu.dev").roles("USER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(pedido())),
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.description").value("teste"))
                .andExpect(jsonPath("$.amount").value(3000.00))
                .andExpect(jsonPath("$.type").value("INCOME"))
                .andExpect(jsonPath("$.sender.id").value(joao.id))
                .andExpect(jsonPath("$.receiver.id").value(maria.id))

            assertEquals(1, transactionRepository.findAll().size)
        }

        @Test
        fun `deve retornar 422 e nao persistir quando o valor excede o saldo do remetente`() {
            mockMvc.perform(
                post("/transactions")
                    .with(user("alice@tregu.dev").roles("USER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(pedido(amount = BigDecimal("9999.00")))),
            )
                .andExpect(status().isUnprocessableContent)
                .andExpect(jsonPath("$.message").value("Account ${joao.id} does not have sufficient balance for this transaction"))

            assertEquals(0, transactionRepository.findAll().size)
        }

        @Test
        fun `deve retornar 422 e nao persistir quando remetente e destinatario sao a mesma conta`() {
            mockMvc.perform(
                post("/transactions")
                    .with(user("alice@tregu.dev").roles("USER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(pedido(senderId = joao.id!!, receiverId = joao.id!!))),
            )
                .andExpect(status().isUnprocessableContent)
                .andExpect(jsonPath("$.message").value("Sender and receiver accounts must be different"))

            assertEquals(0, transactionRepository.findAll().size)
        }
    }

    @Nested
    inner class Listar {

        @Test
        fun `deve listar vazio quando nao ha transacoes`() {
            mockMvc.perform(get("/transactions").with(user("alice@tregu.dev").roles("USER")))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(0))
        }

        @Test
        fun `deve listar todas as transacoes criadas`() {
            transactionRepository.save(
                TransactionEntity("Salário", BigDecimal("3000.00"), TransactionType.INCOME, LocalDateTime.now(), joao, maria),
            )
            transactionRepository.save(
                TransactionEntity("Aluguel", BigDecimal("1200.00"), TransactionType.EXPENSE, LocalDateTime.now(), maria, joao),
            )

            mockMvc.perform(get("/transactions").with(user("alice@tregu.dev").roles("USER")))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(2))
        }
    }

    @Nested
    inner class BuscarPorId {

        @Test
        fun `deve retornar transacao por id`() {
            val salva = transactionRepository.save(
                TransactionEntity("Salário", BigDecimal("5000.00"), TransactionType.INCOME, LocalDateTime.now(), joao, maria),
            )

            mockMvc.perform(get("/transactions/${salva.id}").with(user("alice@tregu.dev").roles("USER")))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(salva.id))
                .andExpect(jsonPath("$.description").value("Salário"))
        }

        @Test
        fun `deve retornar 404 para id inexistente`() {
            mockMvc.perform(get("/transactions/999999").with(user("alice@tregu.dev").roles("USER")))
                .andExpect(status().isNotFound)
        }
    }
}
