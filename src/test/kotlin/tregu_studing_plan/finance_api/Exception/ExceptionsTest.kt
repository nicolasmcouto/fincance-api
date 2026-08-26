package tregu_studing_plan.finance_api.Exception

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** As mensagens de erro chegam ao cliente pelo `ApiError`; sao contrato, nao detalhe interno. */
class ExceptionsTest {

    @Test
    fun `AccountNotFoundException nomeia a conta procurada`() {
        assertEquals("Account 42 not found", AccountNotFoundException(42L).message)
    }

    @Test
    fun `TransactionNotFoundException nomeia a transacao procurada`() {
        assertEquals("Transaction 7 not found", TransactionNotFoundException(7L).message)
    }

    @Test
    fun `InsufficientBalanceException nomeia a conta sem saldo`() {
        assertEquals(
            "Account 3 does not have sufficient balance for this transaction",
            InsufficientBalanceException(3L).message,
        )
    }

    @Test
    fun `InsufficientBalanceException tolera conta ainda sem id`() {
        assertEquals(
            "Account null does not have sufficient balance for this transaction",
            InsufficientBalanceException(null).message,
        )
    }

    @Test
    fun `SameAccountTransferException explica a regra`() {
        assertEquals("Sender and receiver accounts must be different", SameAccountTransferException().message)
    }

    @Test
    fun `todas sao RuntimeException - nao obrigam try-catch e disparam rollback`() {
        val exceptions = listOf<Exception>(
            AccountNotFoundException(1L),
            TransactionNotFoundException(1L),
            InsufficientBalanceException(1L),
            SameAccountTransferException(),
        )
        exceptions.forEach { assertTrue(it is RuntimeException, "${it::class.simpleName} deveria ser RuntimeException") }
    }
}
