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
import tregu_studing_plan.finance_api.Domain.Dto.TransactionRequest
import tregu_studing_plan.finance_api.Domain.Entity.TransactionEntity
import tregu_studing_plan.finance_api.Domain.Enum.TransactionType
import tregu_studing_plan.finance_api.Exception.AccountNotFoundException
import tregu_studing_plan.finance_api.Exception.InsufficientBalanceException
import tregu_studing_plan.finance_api.Exception.SameAccountTransferException
import tregu_studing_plan.finance_api.Exception.TransactionNotFoundException
import tregu_studing_plan.finance_api.Service.AccountService
import tregu_studing_plan.finance_api.Service.TransactionService
import tregu_studing_plan.finance_api.support.Fixtures
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class TransactionControllerTest {

    private val transactionService = mockk<TransactionService>()
    private val accountService = mockk<AccountService>()
    private val mvc = Fixtures.standaloneMvc(TransactionController(transactionService, accountService))

    private val alice = Fixtures.account(id = 1L, ownerName = "Alice", balance = "100.00")
    private val bob = Fixtures.account(id = 2L, ownerName = "Bob", balance = "50.00")

    private fun pedido(
        senderId: Long = 1L,
        receiverId: Long = 2L,
        amount: String = "30.00",
        description: String = "Transferencia",
        type: TransactionType = TransactionType.EXPENSE,
    ) = TransactionRequest(
        description = description,
        amount = BigDecimal(amount),
        type = type,
        date = Fixtures.FIXED_DATE,
        senderId = senderId,
        receiverId = receiverId,
    )

    @Nested
    inner class ListarTodas {

        @Test
        fun `GET transactions responde 200 com a lista aninhando as contas`() {
            every { transactionService.findAll() } returns
                listOf(Fixtures.transaction(alice, bob, id = 10L, amount = "30.00"))

            mvc.perform(get("/transactions"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].amount").value(30.00))
                .andExpect(jsonPath("$[0].type").value("EXPENSE"))
                .andExpect(jsonPath("$[0].date").value("2026-01-15T10:30:00"))
                .andExpect(jsonPath("$[0].sender.id").value(1))
                .andExpect(jsonPath("$[0].receiver.id").value(2))
        }

        @Test
        fun `GET transactions responde 200 com lista vazia`() {
            every { transactionService.findAll() } returns emptyList()

            mvc.perform(get("/transactions"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(0))
        }
    }

    @Nested
    inner class BuscarPorId {

        @Test
        fun `GET transactions id responde 200 com a transacao`() {
            every { transactionService.findById(10L) } returns
                Fixtures.transaction(alice, bob, id = 10L, description = "Aluguel")

            mvc.perform(get("/transactions/10"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.description").value("Aluguel"))
        }

        @Test
        fun `GET transactions id responde 404 quando a transacao nao existe`() {
            every { transactionService.findById(99L) } throws TransactionNotFoundException(99L)

            mvc.perform(get("/transactions/99"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Transaction 99 not found"))
        }
    }

    @Nested
    inner class Criar {

        @Test
        fun `POST transactions responde 201 com a transacao criada`() {
            every { accountService.findById(1L) } returns alice
            every { accountService.findById(2L) } returns bob
            every { transactionService.create(any()) } returns
                Fixtures.transaction(alice, bob, id = 10L, amount = "30.00")

            mvc.perform(
                post("/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(Fixtures.toJson(pedido())),
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.amount").value(30.00))
        }

        @Test
        fun `POST transactions resolve senderId e receiverId em contas reais`() {
            val capturada = slot<TransactionEntity>()
            every { accountService.findById(1L) } returns alice
            every { accountService.findById(2L) } returns bob
            every { transactionService.create(capture(capturada)) } answers { firstArg() }

            mvc.perform(
                post("/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(Fixtures.toJson(pedido(description = "Aluguel", amount = "30.00"))),
            ).andExpect(status().isCreated)

            assertSame(alice, capturada.captured.sender)
            assertSame(bob, capturada.captured.receiver)
            assertEquals("Aluguel", capturada.captured.description)
            assertEquals(BigDecimal("30.00"), capturada.captured.amount)
            assertEquals(Fixtures.FIXED_DATE, capturada.captured.date)
            assertNull(capturada.captured.id, "o id e do banco, nunca do cliente")
            verify(exactly = 1) { accountService.findById(1L) }
            verify(exactly = 1) { accountService.findById(2L) }
        }

        @Test
        fun `POST transactions responde 404 quando o remetente nao existe`() {
            every { accountService.findById(404L) } throws AccountNotFoundException(404L)
            every { accountService.findById(2L) } returns bob

            mvc.perform(
                post("/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(Fixtures.toJson(pedido(senderId = 404L))),
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.message").value("Account 404 not found"))

            verify(exactly = 0) { transactionService.create(any()) }
        }

        @Test
        fun `POST transactions responde 404 quando o destinatario nao existe`() {
            every { accountService.findById(1L) } returns alice
            every { accountService.findById(404L) } throws AccountNotFoundException(404L)

            mvc.perform(
                post("/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(Fixtures.toJson(pedido(receiverId = 404L))),
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.message").value("Account 404 not found"))

            verify(exactly = 0) { transactionService.create(any()) }
        }

        @Test
        fun `POST transactions responde 422 quando o saldo e insuficiente`() {
            every { accountService.findById(1L) } returns alice
            every { accountService.findById(2L) } returns bob
            every { transactionService.create(any()) } throws InsufficientBalanceException(1L)

            mvc.perform(
                post("/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(Fixtures.toJson(pedido(amount = "9999.00"))),
            )
                .andExpect(status().isUnprocessableContent)
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value("Account 1 does not have sufficient balance for this transaction"))
        }

        @Test
        fun `POST transactions responde 422 quando remetente e destinatario sao a mesma conta`() {
            every { accountService.findById(1L) } returns alice
            every { transactionService.create(any()) } throws SameAccountTransferException()

            mvc.perform(
                post("/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(Fixtures.toJson(pedido(senderId = 1L, receiverId = 1L))),
            )
                .andExpect(status().isUnprocessableContent)
                .andExpect(jsonPath("$.message").value("Sender and receiver accounts must be different"))
        }

        @Test
        fun `POST transactions responde 400 quando o tipo de transacao e invalido`() {
            val json = """
                {"description":"x","amount":30.00,"type":"TRANSFER",
                 "date":"2026-01-15T10:30:00","senderId":1,"receiverId":2}
            """.trimIndent()

            mvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest)

            verify(exactly = 0) { transactionService.create(any()) }
        }

        @Test
        fun `POST transactions responde 400 quando a data nao esta no formato ISO`() {
            val json = """
                {"description":"x","amount":30.00,"type":"EXPENSE",
                 "date":"15-01-2026","senderId":1,"receiverId":2}
            """.trimIndent()

            mvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest)

            verify(exactly = 0) { transactionService.create(any()) }
        }
    }

    @Nested
    inner class Atualizar {

        @Test
        fun `PUT transactions id responde 200 com a transacao atualizada`() {
            every { accountService.findById(1L) } returns alice
            every { accountService.findById(2L) } returns bob
            every { transactionService.update(10L, any()) } returns
                Fixtures.transaction(alice, bob, id = 10L, amount = "50.00", description = "Corrigido")

            mvc.perform(
                put("/transactions/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(Fixtures.toJson(pedido(amount = "50.00", description = "Corrigido"))),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.amount").value(50.00))
                .andExpect(jsonPath("$.description").value("Corrigido"))
        }

        @Test
        fun `PUT transactions id encaminha o id do path para o service`() {
            every { accountService.findById(1L) } returns alice
            every { accountService.findById(2L) } returns bob
            every { transactionService.update(42L, any()) } answers { secondArg() }

            mvc.perform(
                put("/transactions/42")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(Fixtures.toJson(pedido())),
            ).andExpect(status().isOk)

            verify(exactly = 1) { transactionService.update(42L, any()) }
        }

        @Test
        fun `PUT transactions id responde 404 quando a transacao nao existe`() {
            every { accountService.findById(1L) } returns alice
            every { accountService.findById(2L) } returns bob
            every { transactionService.update(99L, any()) } throws TransactionNotFoundException(99L)

            mvc.perform(
                put("/transactions/99")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(Fixtures.toJson(pedido())),
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.message").value("Transaction 99 not found"))
        }

        @Test
        fun `PUT transactions id responde 422 quando o novo valor excede o saldo`() {
            every { accountService.findById(1L) } returns alice
            every { accountService.findById(2L) } returns bob
            every { transactionService.update(10L, any()) } throws InsufficientBalanceException(1L)

            mvc.perform(
                put("/transactions/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(Fixtures.toJson(pedido(amount = "9999.00"))),
            ).andExpect(status().isUnprocessableContent)
        }
    }

    @Nested
    inner class Excluir {

        @Test
        fun `DELETE transactions id responde 204 sem corpo`() {
            every { transactionService.delete(10L) } returns Unit

            mvc.perform(delete("/transactions/10"))
                .andExpect(status().isNoContent)
                .andExpect(content().string(""))

            verify(exactly = 1) { transactionService.delete(10L) }
        }

        @Test
        fun `DELETE transactions id responde 404 quando a transacao nao existe`() {
            every { transactionService.delete(99L) } throws TransactionNotFoundException(99L)

            mvc.perform(delete("/transactions/99"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.message").value("Transaction 99 not found"))
        }
    }
}
