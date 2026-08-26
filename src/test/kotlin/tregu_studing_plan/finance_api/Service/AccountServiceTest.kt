package tregu_studing_plan.finance_api.Service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tregu_studing_plan.finance_api.Domain.Entity.AccountEntity
import tregu_studing_plan.finance_api.Domain.Enum.AccountType
import tregu_studing_plan.finance_api.Domain.Repository.AccountRepository
import tregu_studing_plan.finance_api.Exception.AccountNotFoundException
import tregu_studing_plan.finance_api.support.Fixtures
import java.math.BigDecimal
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AccountServiceTest {

    private val repository = mockk<AccountRepository>()
    private val service = AccountService(repository)

    @Nested
    inner class FindAll {

        @Test
        fun `devolve tudo que o repositorio retorna`() {
            val accounts = listOf(Fixtures.account(id = 1L), Fixtures.account(id = 2L, ownerName = "Bob"))
            every { repository.findAll() } returns accounts

            assertEquals(accounts, service.findAll())
            verify(exactly = 1) { repository.findAll() }
        }

        @Test
        fun `devolve lista vazia quando nao ha contas`() {
            every { repository.findAll() } returns emptyList()

            assertTrue(service.findAll().isEmpty())
        }
    }

    @Nested
    inner class FindById {

        @Test
        fun `devolve a conta quando ela existe`() {
            val account = Fixtures.account(id = 42L)
            every { repository.findById(42L) } returns Optional.of(account)

            assertSame(account, service.findById(42L))
        }

        @Test
        fun `lanca AccountNotFoundException quando a conta nao existe`() {
            every { repository.findById(42L) } returns Optional.empty()

            val ex = assertFailsWith<AccountNotFoundException> { service.findById(42L) }
            assertEquals("Account 42 not found", ex.message)
        }
    }

    @Nested
    inner class Create {

        @Test
        fun `delega ao repositorio e devolve a entidade persistida`() {
            val nova = AccountEntity("Alice", AccountType.CHECKING, BigDecimal("100.00"))
            val salva = Fixtures.account(id = 1L)
            every { repository.save(nova) } returns salva

            assertSame(salva, service.create(nova))
            verify(exactly = 1) { repository.save(nova) }
        }

        @Test
        fun `nao consulta o banco antes de criar`() {
            val nova = AccountEntity("Alice", AccountType.CHECKING, BigDecimal("100.00"))
            every { repository.save(any()) } returns nova

            service.create(nova)

            verify(exactly = 0) { repository.findById(any()) }
        }
    }

    @Nested
    inner class Update {

        @Test
        fun `sobrescreve ownerName, type e balance da conta existente`() {
            val existente = Fixtures.account(id = 5L, ownerName = "Alice", type = AccountType.CHECKING, balance = "100.00")
            val alteracao = Fixtures.account(id = null, ownerName = "Alice Silva", type = AccountType.SAVINGS, balance = "999.99")
            every { repository.findById(5L) } returns Optional.of(existente)
            every { repository.save(existente) } returns existente

            val resultado = service.update(5L, alteracao)

            assertSame(existente, resultado)
            assertEquals("Alice Silva", resultado.ownerName)
            assertEquals(AccountType.SAVINGS, resultado.type)
            assertEquals(BigDecimal("999.99"), resultado.balance)
        }

        @Test
        fun `preserva id e createdAt da conta original`() {
            val criadaEm = Fixtures.FIXED_DATE
            val existente = Fixtures.account(id = 5L, createdAt = criadaEm)
            val alteracao = Fixtures.account(id = 999L, ownerName = "Outro", createdAt = criadaEm.plusYears(1))
            every { repository.findById(5L) } returns Optional.of(existente)
            every { repository.save(existente) } returns existente

            val resultado = service.update(5L, alteracao)

            assertEquals(5L, resultado.id, "o id do path deve prevalecer sobre o id do corpo")
            assertEquals(criadaEm, resultado.createdAt, "createdAt e imutavel apos a criacao")
        }

        @Test
        fun `persiste a entidade gerenciada, nao a recebida no corpo`() {
            val existente = Fixtures.account(id = 5L)
            val alteracao = Fixtures.account(id = null, ownerName = "Nova")
            every { repository.findById(5L) } returns Optional.of(existente)
            every { repository.save(existente) } returns existente

            service.update(5L, alteracao)

            verify(exactly = 1) { repository.save(existente) }
            verify(exactly = 0) { repository.save(alteracao) }
        }

        @Test
        fun `propaga AccountNotFoundException e nao salva nada`() {
            every { repository.findById(5L) } returns Optional.empty()

            assertFailsWith<AccountNotFoundException> { service.update(5L, Fixtures.account()) }
            verify(exactly = 0) { repository.save(any()) }
        }
    }

    @Nested
    inner class Delete {

        @Test
        fun `remove a conta encontrada`() {
            val account = Fixtures.account(id = 5L)
            every { repository.findById(5L) } returns Optional.of(account)
            every { repository.delete(account) } returns Unit

            service.delete(5L)

            verify(exactly = 1) { repository.delete(account) }
        }

        @Test
        fun `propaga AccountNotFoundException e nao remove nada`() {
            every { repository.findById(5L) } returns Optional.empty()

            assertFailsWith<AccountNotFoundException> { service.delete(5L) }
            verify(exactly = 0) { repository.delete(any<AccountEntity>()) }
        }
    }
}
