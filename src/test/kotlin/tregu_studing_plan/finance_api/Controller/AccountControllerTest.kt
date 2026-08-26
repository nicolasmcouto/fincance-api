package tregu_studing_plan.finance_api.Controller

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tregu_studing_plan.finance_api.Domain.Dto.AccountRequest
import tregu_studing_plan.finance_api.Domain.Entity.AccountEntity
import tregu_studing_plan.finance_api.Domain.Enum.AccountType
import tregu_studing_plan.finance_api.Exception.AccountNotFoundException
import tregu_studing_plan.finance_api.Service.AccountService
import tregu_studing_plan.finance_api.support.Fixtures
import java.math.BigDecimal
import kotlin.test.assertEquals

class AccountControllerTest {

    private val accountService = mockk<AccountService>()
    private val mvc = Fixtures.standaloneMvc(AccountController(accountService))

    @Nested
    inner class ListarTodas {

        @Test
        fun `GET accounts responde 200 com a lista`() {
            every { accountService.findAll() } returns listOf(
                Fixtures.account(id = 1L, ownerName = "Alice", balance = "100.00"),
                Fixtures.account(id = 2L, ownerName = "Bob", type = AccountType.SAVINGS, balance = "250.50"),
            )

            mvc.perform(get("/accounts"))
                .andExpect(status().isOk)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].ownerName").value("Alice"))
                .andExpect(jsonPath("$[0].type").value("CHECKING"))
                .andExpect(jsonPath("$[0].balance").value(100.00))
                .andExpect(jsonPath("$[1].type").value("SAVINGS"))
        }

        @Test
        fun `GET accounts responde 200 com lista vazia`() {
            every { accountService.findAll() } returns emptyList()

            mvc.perform(get("/accounts"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(0))
        }
    }

    @Nested
    inner class BuscarPorId {

        @Test
        fun `GET accounts id responde 200 com a conta`() {
            every { accountService.findById(1L) } returns Fixtures.account(id = 1L, ownerName = "Alice")

            mvc.perform(get("/accounts/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.ownerName").value("Alice"))
        }

        @Test
        fun `GET accounts id responde 404 quando a conta nao existe`() {
            every { accountService.findById(99L) } throws AccountNotFoundException(99L)

            mvc.perform(get("/accounts/99"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Account 99 not found"))
                .andExpect(jsonPath("$.timestamp").exists())
        }
    }

    @Nested
    inner class Criar {

        @Test
        fun `POST accounts responde 201 com a conta criada`() {
            every { accountService.create(any()) } returns Fixtures.account(id = 7L, ownerName = "Alice", balance = "100.00")

            mvc.perform(
                post("/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(Fixtures.toJson(AccountRequest("Alice", AccountType.CHECKING, BigDecimal("100.00")))),
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.ownerName").value("Alice"))
        }

        @Test
        fun `POST accounts converte o DTO em entidade sem id`() {
            val capturada = slot<AccountEntity>()
            every { accountService.create(capture(capturada)) } returns Fixtures.account(id = 7L)

            mvc.perform(
                post("/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(Fixtures.toJson(AccountRequest("Bob", AccountType.SAVINGS, BigDecimal("42.42")))),
            ).andExpect(status().isCreated)

            assertEquals("Bob", capturada.captured.ownerName)
            assertEquals(AccountType.SAVINGS, capturada.captured.type)
            assertEquals(BigDecimal("42.42"), capturada.captured.balance)
            assertEquals(null, capturada.captured.id, "o id e do banco, nunca do cliente")
        }

        @Test
        fun `POST accounts responde 400 quando o tipo de conta e invalido`() {
            val json = """{"ownerName":"Alice","type":"CORRENTE","balance":100.00}"""

            mvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest)

            verify(exactly = 0) { accountService.create(any()) }
        }

        @Test
        fun `POST accounts responde 400 quando o corpo nao e JSON valido`() {
            mvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest)

            verify(exactly = 0) { accountService.create(any()) }
        }
    }

    @Nested
    inner class Atualizar {

        @Test
        fun `PUT accounts id responde 200 com a conta atualizada`() {
            every { accountService.update(1L, any()) } returns
                Fixtures.account(id = 1L, ownerName = "Alice Silva", balance = "999.99")

            mvc.perform(
                put("/accounts/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(Fixtures.toJson(AccountRequest("Alice Silva", AccountType.CHECKING, BigDecimal("999.99")))),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.ownerName").value("Alice Silva"))
                .andExpect(jsonPath("$.balance").value(999.99))
        }

        @Test
        fun `PUT accounts id encaminha o id do path para o service`() {
            val capturada = slot<AccountEntity>()
            every { accountService.update(5L, capture(capturada)) } returns Fixtures.account(id = 5L)

            mvc.perform(
                put("/accounts/5")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(Fixtures.toJson(AccountRequest("Carol", AccountType.SAVINGS, BigDecimal("10.00")))),
            ).andExpect(status().isOk)

            verify(exactly = 1) { accountService.update(5L, any()) }
            assertEquals("Carol", capturada.captured.ownerName)
        }

        @Test
        fun `PUT accounts id responde 404 quando a conta nao existe`() {
            every { accountService.update(99L, any()) } throws AccountNotFoundException(99L)

            mvc.perform(
                put("/accounts/99")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(Fixtures.toJson(AccountRequest("Alice", AccountType.CHECKING, BigDecimal("1.00")))),
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.message").value("Account 99 not found"))
        }
    }

    @Nested
    inner class Excluir {

        @Test
        fun `DELETE accounts id responde 204 sem corpo`() {
            every { accountService.delete(1L) } returns Unit

            mvc.perform(delete("/accounts/1"))
                .andExpect(status().isNoContent)
                .andExpect(content().string(""))

            verify(exactly = 1) { accountService.delete(1L) }
        }

        @Test
        fun `DELETE accounts id responde 404 quando a conta nao existe`() {
            every { accountService.delete(99L) } throws AccountNotFoundException(99L)

            mvc.perform(delete("/accounts/99"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.message").value("Account 99 not found"))
        }
    }
}
