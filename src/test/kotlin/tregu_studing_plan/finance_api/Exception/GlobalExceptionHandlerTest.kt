package tregu_studing_plan.finance_api.Exception

import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    fun `conta inexistente vira 404 Not Found`() {
        val response = handler.handleAccountNotFound(AccountNotFoundException(42L))

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        val body = assertNotNull(response.body)
        assertEquals(404, body.status)
        assertEquals("Not Found", body.error)
        assertEquals("Account 42 not found", body.message)
    }

    @Test
    fun `transacao inexistente vira 404 Not Found`() {
        val response = handler.handleTransactionNotFound(TransactionNotFoundException(7L))

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        val body = assertNotNull(response.body)
        assertEquals(404, body.status)
        assertEquals("Not Found", body.error)
        assertEquals("Transaction 7 not found", body.message)
    }

    @Test
    fun `saldo insuficiente vira 422 Unprocessable Entity`() {
        val response = handler.handleInsufficientBalance(InsufficientBalanceException(3L))

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.statusCode)
        val body = assertNotNull(response.body)
        assertEquals(422, body.status)
        assertEquals("Unprocessable Entity", body.error)
        assertEquals("Account 3 does not have sufficient balance for this transaction", body.message)
    }

    @Test
    fun `transferencia para a propria conta vira 422 Unprocessable Entity`() {
        val response = handler.handleSameAccountTransfer(SameAccountTransferException())

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.statusCode)
        val body = assertNotNull(response.body)
        assertEquals(422, body.status)
        assertEquals("Unprocessable Entity", body.error)
        assertEquals("Sender and receiver accounts must be different", body.message)
    }

    @Test
    fun `o corpo carrega o instante do erro`() {
        val antes = LocalDateTime.now()
        val body = assertNotNull(handler.handleAccountNotFound(AccountNotFoundException(1L)).body)
        val depois = LocalDateTime.now()

        assertTrue(
            !body.timestamp.isBefore(antes) && !body.timestamp.isAfter(depois),
            "timestamp=${body.timestamp} deveria estar entre $antes e $depois",
        )
    }

    @Test
    fun `ApiError e um data class - comparavel campo a campo`() {
        val instante = LocalDateTime.of(2026, 1, 1, 0, 0)
        assertEquals(
            ApiError(instante, 404, "Not Found", "x"),
            ApiError(instante, 404, "Not Found", "x"),
        )
    }
}
