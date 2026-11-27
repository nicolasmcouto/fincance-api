package tregu_studing_plan.finance_api.Domain.Dto

import jakarta.validation.Validation
import org.junit.jupiter.api.Test
import tregu_studing_plan.finance_api.Domain.Enum.AccountType
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AccountRequestTest {

    // O mesmo "fiscal" que o @Valid usa por baixo dos panos
    private val validator = Validation.buildDefaultValidatorFactory().validator

    // Nível 1: o menor teste possível
    @Test
    fun `dois mais dois da quatro`() {
        val resultado = 2 + 2
        assertEquals(4, resultado)
    }

    // Nível 2: o DTO direto, sem servidor e sem mock
    @Test
    fun `conta valida nao tem erros`() {
        val request = AccountRequest(ownerName = "Ana", type = AccountType.CHECKING, balance = BigDecimal("10"))

        val erros = validator.validate(request)

        assertTrue(erros.isEmpty())
    }

    @Test
    fun `nome vazio da um erro no ownerName`() {
        val request = AccountRequest(ownerName = "", type = AccountType.CHECKING, balance = BigDecimal("10"))

        val erros = validator.validate(request)

        assertEquals(1, erros.size)
        assertEquals("ownerName", erros.first().propertyPath.toString())
    }

    @Test
    fun `nome so com espacos da erro`() {
        val request = AccountRequest(ownerName = "   ", type = AccountType.CHECKING, balance = BigDecimal("10"))

        val erros = validator.validate(request)

        assertEquals(1, erros.size)
        assertEquals("ownerName", erros.first().propertyPath.toString())
    }

    @Test
    fun `saldo negativo da erro no balance`() {
        val request = AccountRequest(ownerName = "Ana", type = AccountType.CHECKING, balance = BigDecimal("-1"))

        val erros = validator.validate(request)

        assertEquals(1, erros.size)
        assertEquals("balance", erros.first().propertyPath.toString())
    }

    @Test
    fun `nome com 41 letras da erro`() {
        val request = AccountRequest(ownerName = "a".repeat(41), type = AccountType.CHECKING, balance = BigDecimal("10"))

        val erros = validator.validate(request)

        assertEquals(1, erros.size)
        assertEquals("ownerName", erros.first().propertyPath.toString())
    }

    @Test
    fun `nome vazio e saldo negativo dao dois erros`() {
        val request = AccountRequest(ownerName = "", type = AccountType.CHECKING, balance = BigDecimal("-1"))

        val erros = validator.validate(request)

        // A ordem dos erros não é garantida, então comparamos como conjunto
        val campos = erros.map { it.propertyPath.toString() }.toSet()
        assertEquals(setOf("ownerName", "balance"), campos)
    }
}
