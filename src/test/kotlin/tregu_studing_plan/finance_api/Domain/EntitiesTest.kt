package tregu_studing_plan.finance_api.Domain

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tregu_studing_plan.finance_api.Domain.Entity.AccountEntity
import tregu_studing_plan.finance_api.Domain.Entity.TransactionEntity
import tregu_studing_plan.finance_api.Domain.Enum.AccountType
import tregu_studing_plan.finance_api.Domain.Enum.TransactionType
import tregu_studing_plan.finance_api.Security.Role
import tregu_studing_plan.finance_api.Security.UserEntity
import tregu_studing_plan.finance_api.support.Fixtures
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class EntitiesTest {

    @Nested
    inner class Account {

        @Test
        fun `nasce sem id ate ser persistida`() {
            val account = AccountEntity("Alice", AccountType.CHECKING, BigDecimal("10.00"))
            assertNull(account.id)
        }

        @Test
        fun `createdAt tem default de agora quando nao informado`() {
            val before = LocalDateTime.now()
            val account = AccountEntity("Alice", AccountType.CHECKING, BigDecimal("10.00"))
            val after = LocalDateTime.now()

            assertTrue(
                !account.createdAt.isBefore(before) && !account.createdAt.isAfter(after),
                "createdAt=${account.createdAt} deveria estar entre $before e $after",
            )
            assertTrue(Duration.between(before, account.createdAt).abs() < Duration.ofMinutes(1))
        }

        @Test
        fun `guarda os campos recebidos no construtor`() {
            val account = Fixtures.account(id = 7L, ownerName = "Bob", type = AccountType.SAVINGS, balance = "250.75")

            assertEquals(7L, account.id)
            assertEquals("Bob", account.ownerName)
            assertEquals(AccountType.SAVINGS, account.type)
            assertEquals(BigDecimal("250.75"), account.balance)
            assertEquals(Fixtures.FIXED_DATE, account.createdAt)
        }

        @Test
        fun `campos mutaveis permitem atualizacao in-place, como o service faz`() {
            val account = Fixtures.account()

            account.ownerName = "Carol"
            account.type = AccountType.SAVINGS
            account.balance = BigDecimal("0.00")

            assertEquals("Carol", account.ownerName)
            assertEquals(AccountType.SAVINGS, account.type)
            assertEquals(BigDecimal("0.00"), account.balance)
        }
    }

    @Nested
    inner class Transaction {

        @Test
        fun `nasce sem id ate ser persistida`() {
            val tx = TransactionEntity(
                description = "x",
                amount = BigDecimal("1.00"),
                type = TransactionType.INCOME,
                date = Fixtures.FIXED_DATE,
                sender = Fixtures.account(id = 1L),
                receiver = Fixtures.account(id = 2L),
            )
            assertNull(tx.id)
        }

        @Test
        fun `mantem referencia para as contas de origem e destino`() {
            val sender = Fixtures.account(id = 1L, ownerName = "Alice")
            val receiver = Fixtures.account(id = 2L, ownerName = "Bob")
            val tx = Fixtures.transaction(sender = sender, receiver = receiver)

            assertSame(sender, tx.sender)
            assertSame(receiver, tx.receiver)
        }

        @Test
        fun `guarda os campos recebidos no construtor`() {
            val tx = Fixtures.transaction(
                sender = Fixtures.account(id = 1L),
                receiver = Fixtures.account(id = 2L),
                id = 99L,
                description = "Aluguel",
                amount = "1200.00",
                type = TransactionType.EXPENSE,
            )

            assertEquals(99L, tx.id)
            assertEquals("Aluguel", tx.description)
            assertEquals(BigDecimal("1200.00"), tx.amount)
            assertEquals(TransactionType.EXPENSE, tx.type)
            assertEquals(Fixtures.FIXED_DATE, tx.date)
        }
    }

    @Nested
    inner class User {

        @Test
        fun `nasce vazia e com o papel USER por padrao`() {
            val user = UserEntity()

            assertNull(user.id)
            assertNull(user.email)
            assertNull(user.passwordHash)
            assertEquals(setOf(Role.USER), user.roles)
        }

        @Test
        fun `aceita multiplos papeis`() {
            val user = Fixtures.user(roles = setOf(Role.USER, Role.ADMIN))
            assertEquals(setOf(Role.USER, Role.ADMIN), user.roles)
        }

        @Test
        fun `roles e um Set - nao duplica o mesmo papel`() {
            val user = UserEntity()
            user.roles.add(Role.USER)
            user.roles.add(Role.USER)

            assertEquals(1, user.roles.size)
        }
    }
}
