package tregu_studing_plan.finance_api.Domain

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tregu_studing_plan.finance_api.Domain.Enum.AccountType
import tregu_studing_plan.finance_api.Domain.Enum.TransactionType
import tregu_studing_plan.finance_api.Security.Role
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EnumsTest {

    @Nested
    inner class AccountTypes {

        @Test
        fun `expoe exatamente CHECKING e SAVINGS, nesta ordem`() {
            assertEquals(listOf("CHECKING", "SAVINGS"), AccountType.entries.map { it.name })
        }

        @Test
        fun `resolve valueOf a partir do nome persistido`() {
            assertEquals(AccountType.CHECKING, AccountType.valueOf("CHECKING"))
            assertEquals(AccountType.SAVINGS, AccountType.valueOf("SAVINGS"))
        }

        @Test
        fun `rejeita nome desconhecido`() {
            assertFailsWith<IllegalArgumentException> { AccountType.valueOf("CORRENTE") }
        }
    }

    @Nested
    inner class TransactionTypes {

        @Test
        fun `expoe exatamente INCOME e EXPENSE, nesta ordem`() {
            assertEquals(listOf("INCOME", "EXPENSE"), TransactionType.entries.map { it.name })
        }

        @Test
        fun `resolve valueOf a partir do nome persistido`() {
            assertEquals(TransactionType.INCOME, TransactionType.valueOf("INCOME"))
            assertEquals(TransactionType.EXPENSE, TransactionType.valueOf("EXPENSE"))
        }

        @Test
        fun `rejeita nome desconhecido`() {
            assertFailsWith<IllegalArgumentException> { TransactionType.valueOf("TRANSFER") }
        }
    }

    @Nested
    inner class Roles {

        @Test
        fun `expoe USER, MANAGER e ADMIN`() {
            assertEquals(listOf("USER", "MANAGER", "ADMIN"), Role.entries.map { it.name })
        }

        @Test
        fun `toAuthority prefixa ROLE_ em todos os papeis`() {
            assertEquals("ROLE_USER", Role.USER.toAuthority())
            assertEquals("ROLE_MANAGER", Role.MANAGER.toAuthority())
            assertEquals("ROLE_ADMIN", Role.ADMIN.toAuthority())
        }

        @Test
        fun `toAuthority nunca duplica o prefixo`() {
            Role.entries.forEach { role ->
                assertEquals(1, Regex("ROLE_").findAll(role.toAuthority()).count(), "papel $role")
            }
        }
    }
}
