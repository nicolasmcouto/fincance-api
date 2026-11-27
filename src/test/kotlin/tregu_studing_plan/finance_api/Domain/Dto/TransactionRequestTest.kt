package tregu_studing_plan.finance_api.Domain.Dto

import jakarta.validation.Validation
import org.junit.jupiter.api.Test
import tregu_studing_plan.finance_api.Domain.Enum.TransactionType
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TransactionRequestTest {

    private val validator = Validation.buildDefaultValidatorFactory().validator

    // Uma transação 100% válida, a base de todos os testes.
    // Cada teste estraga um campo só, usando copy().
    private val valida = TransactionRequest(
        description = "Aluguel",
        amount = BigDecimal("100.00"),
        type = TransactionType.EXPENSE,
        date = LocalDateTime.now().minusDays(1),
        senderId = 1,
        receiverId = 2,
    )

    // Devolve os nomes dos campos que deram erro, ex.: setOf("amount")
    private fun camposComErro(request: TransactionRequest) =
        validator.validate(request).map { it.propertyPath.toString() }.toSet()

    @Test
    fun `transacao valida nao tem erros`() {
        assertTrue(camposComErro(valida).isEmpty())
    }

    // --- amount ---

    @Test
    fun `valor negativo da erro no amount`() {
        val request = valida.copy(amount = BigDecimal("-100"))
        assertEquals(setOf("amount"), camposComErro(request))
    }

    @Test
    fun `valor zero da erro no amount`() {
        val request = valida.copy(amount = BigDecimal.ZERO)
        assertEquals(setOf("amount"), camposComErro(request))
    }

    @Test
    fun `valor com tres casas decimais da erro no amount`() {
        val request = valida.copy(amount = BigDecimal("10.555"))
        assertEquals(setOf("amount"), camposComErro(request))
    }

    // --- description ---

    @Test
    fun `descricao vazia da erro`() {
        val request = valida.copy(description = "")
        assertEquals(setOf("description"), camposComErro(request))
    }

    @Test
    fun `descricao so com espacos da erro`() {
        val request = valida.copy(description = "   ")
        assertEquals(setOf("description"), camposComErro(request))
    }

    @Test
    fun `descricao com 256 caracteres da erro`() {
        val request = valida.copy(description = "a".repeat(256))
        assertEquals(setOf("description"), camposComErro(request))
    }

    // --- date ---

    @Test
    fun `data no futuro da erro`() {
        val request = valida.copy(date = LocalDateTime.now().plusDays(1))
        assertEquals(setOf("date"), camposComErro(request))
    }

    // --- ids ---

    @Test
    fun `senderId zero da erro`() {
        val request = valida.copy(senderId = 0)
        assertEquals(setOf("senderId"), camposComErro(request))
    }

    @Test
    fun `receiverId negativo da erro`() {
        val request = valida.copy(receiverId = -1)
        assertEquals(setOf("receiverId"), camposComErro(request))
    }

    // --- regra de negócio: não é papel do DTO ---

    @Test
    fun `mesma conta passa no DTO porque quem barra e o service`() {
        // Os dois ids são positivos, então o formato está certo (nada de 400).
        // Quem recusa é o TransactionService, com SameAccountTransferException (422).
        val request = valida.copy(senderId = 1, receiverId = 1)
        assertTrue(camposComErro(request).isEmpty())
    }
}
