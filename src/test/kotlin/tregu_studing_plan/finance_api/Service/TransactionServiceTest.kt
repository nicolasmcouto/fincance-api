package tregu_studing_plan.finance_api.Service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tregu_studing_plan.finance_api.Domain.Entity.AccountEntity
import tregu_studing_plan.finance_api.Domain.Entity.TransactionEntity
import tregu_studing_plan.finance_api.Domain.Enum.TransactionType
import tregu_studing_plan.finance_api.Domain.Repository.AccountRepository
import tregu_studing_plan.finance_api.Domain.Repository.TransactionRepository
import tregu_studing_plan.finance_api.Exception.InsufficientBalanceException
import tregu_studing_plan.finance_api.Exception.SameAccountTransferException
import tregu_studing_plan.finance_api.Exception.TransactionNotFoundException
import tregu_studing_plan.finance_api.support.Fixtures
import java.math.BigDecimal
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TransactionServiceTest {

    private val transactionRepository = mockk<TransactionRepository>()
    private val accountRepository = mockk<AccountRepository>()
    private val service = TransactionService(transactionRepository, accountRepository)

    /** Deixa `accountRepository.save` passivo: o interesse esta no efeito sobre o saldo. */
    private fun permitirSalvarContas() {
        every { accountRepository.save(any<AccountEntity>()) } answers { firstArg() }
    }

    @Nested
    inner class FindAll {

        @Test
        fun `devolve tudo que o repositorio retorna`() {
            val txs = listOf(Fixtures.transaction(Fixtures.account(id = 1L), Fixtures.account(id = 2L)))
            every { transactionRepository.findAll() } returns txs

            assertEquals(txs, service.findAll())
        }

        @Test
        fun `devolve lista vazia quando nao ha transacoes`() {
            every { transactionRepository.findAll() } returns emptyList()

            assertTrue(service.findAll().isEmpty())
        }
    }

    @Nested
    inner class FindById {

        @Test
        fun `devolve a transacao quando ela existe`() {
            val tx = Fixtures.transaction(Fixtures.account(id = 1L), Fixtures.account(id = 2L), id = 7L)
            every { transactionRepository.findById(7L) } returns Optional.of(tx)

            assertSame(tx, service.findById(7L))
        }

        @Test
        fun `lanca TransactionNotFoundException quando a transacao nao existe`() {
            every { transactionRepository.findById(7L) } returns Optional.empty()

            val ex = assertFailsWith<TransactionNotFoundException> { service.findById(7L) }
            assertEquals("Transaction 7 not found", ex.message)
        }
    }

    @Nested
    inner class Create {

        @Test
        fun `debita o remetente e credita o destinatario`() {
            val sender = Fixtures.account(id = 1L, balance = "100.00")
            val receiver = Fixtures.account(id = 2L, balance = "50.00")
            val tx = Fixtures.transaction(sender, receiver, amount = "30.00")
            permitirSalvarContas()
            every { transactionRepository.save(tx) } returns tx

            service.create(tx)

            assertEquals(BigDecimal("70.00"), sender.balance)
            assertEquals(BigDecimal("80.00"), receiver.balance)
        }

        @Test
        fun `persiste as duas contas e a transacao`() {
            val sender = Fixtures.account(id = 1L, balance = "100.00")
            val receiver = Fixtures.account(id = 2L, balance = "50.00")
            val tx = Fixtures.transaction(sender, receiver, amount = "30.00")
            permitirSalvarContas()
            every { transactionRepository.save(tx) } returns tx

            service.create(tx)

            verify(exactly = 1) { accountRepository.save(sender) }
            verify(exactly = 1) { accountRepository.save(receiver) }
            verify(exactly = 1) { transactionRepository.save(tx) }
        }

        @Test
        fun `devolve a entidade retornada pelo repositorio`() {
            val sender = Fixtures.account(id = 1L, balance = "100.00")
            val receiver = Fixtures.account(id = 2L, balance = "50.00")
            val tx = Fixtures.transaction(sender, receiver, id = null, amount = "30.00")
            val persistida = Fixtures.transaction(sender, receiver, id = 99L, amount = "30.00")
            permitirSalvarContas()
            every { transactionRepository.save(tx) } returns persistida

            assertSame(persistida, service.create(tx))
        }

        @Test
        fun `aceita transferencia que zera o saldo do remetente`() {
            val sender = Fixtures.account(id = 1L, balance = "100.00")
            val receiver = Fixtures.account(id = 2L, balance = "0.00")
            val tx = Fixtures.transaction(sender, receiver, amount = "100.00")
            permitirSalvarContas()
            every { transactionRepository.save(tx) } returns tx

            service.create(tx)

            assertEquals(0, BigDecimal.ZERO.compareTo(sender.balance), "saldo do remetente deveria zerar")
            assertEquals(BigDecimal("100.00"), receiver.balance)
        }

        @Test
        fun `preserva as casas decimais na movimentacao`() {
            val sender = Fixtures.account(id = 1L, balance = "100.00")
            val receiver = Fixtures.account(id = 2L, balance = "0.05")
            val tx = Fixtures.transaction(sender, receiver, amount = "10.50")
            permitirSalvarContas()
            every { transactionRepository.save(tx) } returns tx

            service.create(tx)

            assertEquals(BigDecimal("89.50"), sender.balance)
            assertEquals(BigDecimal("10.55"), receiver.balance)
        }

        @Test
        fun `rejeita transferencia para a propria conta`() {
            val conta = Fixtures.account(id = 1L, balance = "100.00")
            val tx = Fixtures.transaction(conta, conta, amount = "30.00")

            val ex = assertFailsWith<SameAccountTransferException> { service.create(tx) }
            assertEquals("Sender and receiver accounts must be different", ex.message)
        }

        @Test
        fun `transferencia para a propria conta nao mexe em saldo nem persiste`() {
            val conta = Fixtures.account(id = 1L, balance = "100.00")
            val tx = Fixtures.transaction(conta, conta, amount = "30.00")

            assertFailsWith<SameAccountTransferException> { service.create(tx) }

            assertEquals(BigDecimal("100.00"), conta.balance)
            verify(exactly = 0) { accountRepository.save(any<AccountEntity>()) }
            verify(exactly = 0) { transactionRepository.save(any<TransactionEntity>()) }
        }

        @Test
        fun `duas contas ainda nao persistidas contam como a mesma conta (ambos os ids nulos)`() {
            val sender = Fixtures.account(id = null, ownerName = "Alice", balance = "100.00")
            val receiver = Fixtures.account(id = null, ownerName = "Bob", balance = "50.00")
            val tx = Fixtures.transaction(sender, receiver, amount = "30.00")

            assertFailsWith<SameAccountTransferException> { service.create(tx) }
        }

        @Test
        fun `rejeita transferencia acima do saldo`() {
            val sender = Fixtures.account(id = 1L, balance = "100.00")
            val receiver = Fixtures.account(id = 2L, balance = "50.00")
            val tx = Fixtures.transaction(sender, receiver, amount = "100.01")

            val ex = assertFailsWith<InsufficientBalanceException> { service.create(tx) }
            assertEquals("Account 1 does not have sufficient balance for this transaction", ex.message)
        }

        @Test
        fun `saldo insuficiente nao mexe em saldo nem persiste`() {
            val sender = Fixtures.account(id = 1L, balance = "100.00")
            val receiver = Fixtures.account(id = 2L, balance = "50.00")
            val tx = Fixtures.transaction(sender, receiver, amount = "500.00")

            assertFailsWith<InsufficientBalanceException> { service.create(tx) }

            assertEquals(BigDecimal("100.00"), sender.balance)
            assertEquals(BigDecimal("50.00"), receiver.balance)
            verify(exactly = 0) { accountRepository.save(any<AccountEntity>()) }
            verify(exactly = 0) { transactionRepository.save(any<TransactionEntity>()) }
        }

        @Test
        fun `conta identica e checada antes do saldo`() {
            val conta = Fixtures.account(id = 1L, balance = "0.00")
            val tx = Fixtures.transaction(conta, conta, amount = "30.00")

            // Saldo tambem seria insuficiente; a regra de conta identica vence.
            assertFailsWith<SameAccountTransferException> { service.create(tx) }
        }
    }

    @Nested
    inner class Update {

        @Test
        fun `estorna o valor antigo e aplica o novo`() {
            // Estado pos-transferencia de 30.00: Alice 70.00, Bob 80.00.
            val sender = Fixtures.account(id = 1L, balance = "70.00")
            val receiver = Fixtures.account(id = 2L, balance = "80.00")
            val existente = Fixtures.transaction(sender, receiver, id = 7L, amount = "30.00")
            val alteracao = Fixtures.transaction(sender, receiver, id = null, amount = "50.00")
            every { transactionRepository.findById(7L) } returns Optional.of(existente)
            permitirSalvarContas()
            every { transactionRepository.save(existente) } returns existente

            service.update(7L, alteracao)

            // Estorno: 100.00 / 50.00 -> nova transferencia de 50.00.
            assertEquals(BigDecimal("50.00"), sender.balance)
            assertEquals(BigDecimal("100.00"), receiver.balance)
        }

        @Test
        fun `copia todos os campos editaveis para a entidade gerenciada`() {
            val sender = Fixtures.account(id = 1L, balance = "70.00")
            val receiver = Fixtures.account(id = 2L, balance = "80.00")
            val existente = Fixtures.transaction(sender, receiver, id = 7L, amount = "30.00")
            val novaData = Fixtures.FIXED_DATE.plusDays(3)
            val alteracao = Fixtures.transaction(
                sender, receiver,
                id = null,
                description = "Aluguel corrigido",
                amount = "40.00",
                type = TransactionType.INCOME,
                date = novaData,
            )
            every { transactionRepository.findById(7L) } returns Optional.of(existente)
            permitirSalvarContas()
            every { transactionRepository.save(existente) } returns existente

            val resultado = service.update(7L, alteracao)

            assertSame(existente, resultado)
            assertEquals(7L, resultado.id, "o id do path deve prevalecer")
            assertEquals("Aluguel corrigido", resultado.description)
            assertEquals(BigDecimal("40.00"), resultado.amount)
            assertEquals(TransactionType.INCOME, resultado.type)
            assertEquals(novaData, resultado.date)
        }

        @Test
        fun `permite inverter remetente e destinatario`() {
            val alice = Fixtures.account(id = 1L, balance = "70.00")
            val bob = Fixtures.account(id = 2L, balance = "80.00")
            val existente = Fixtures.transaction(alice, bob, id = 7L, amount = "30.00")
            val alteracao = Fixtures.transaction(bob, alice, id = null, amount = "30.00")
            every { transactionRepository.findById(7L) } returns Optional.of(existente)
            permitirSalvarContas()
            every { transactionRepository.save(existente) } returns existente

            val resultado = service.update(7L, alteracao)

            // Estorno leva a 100.00 / 50.00; depois Bob envia 30.00 para Alice.
            assertEquals(BigDecimal("130.00"), alice.balance)
            assertEquals(BigDecimal("20.00"), bob.balance)
            assertSame(bob, resultado.sender)
            assertSame(alice, resultado.receiver)
        }

        @Test
        fun `persiste a entidade gerenciada, nao a recebida no corpo`() {
            val sender = Fixtures.account(id = 1L, balance = "70.00")
            val receiver = Fixtures.account(id = 2L, balance = "80.00")
            val existente = Fixtures.transaction(sender, receiver, id = 7L, amount = "30.00")
            val alteracao = Fixtures.transaction(sender, receiver, id = null, amount = "30.00")
            every { transactionRepository.findById(7L) } returns Optional.of(existente)
            permitirSalvarContas()
            every { transactionRepository.save(existente) } returns existente

            service.update(7L, alteracao)

            verify(exactly = 1) { transactionRepository.save(existente) }
            verify(exactly = 0) { transactionRepository.save(alteracao) }
        }

        @Test
        fun `propaga TransactionNotFoundException e nao mexe em nada`() {
            every { transactionRepository.findById(7L) } returns Optional.empty()

            assertFailsWith<TransactionNotFoundException> {
                service.update(7L, Fixtures.transaction(Fixtures.account(id = 1L), Fixtures.account(id = 2L)))
            }

            verify(exactly = 0) { accountRepository.save(any<AccountEntity>()) }
            verify(exactly = 0) { transactionRepository.save(any<TransactionEntity>()) }
        }

        @Test
        fun `rejeita novo valor acima do saldo estornado e nao salva a transacao`() {
            val sender = Fixtures.account(id = 1L, balance = "70.00")
            val receiver = Fixtures.account(id = 2L, balance = "80.00")
            val existente = Fixtures.transaction(sender, receiver, id = 7L, amount = "30.00")
            val alteracao = Fixtures.transaction(sender, receiver, id = null, amount = "1000.00")
            every { transactionRepository.findById(7L) } returns Optional.of(existente)
            permitirSalvarContas()

            assertFailsWith<InsufficientBalanceException> { service.update(7L, alteracao) }

            verify(exactly = 0) { transactionRepository.save(any<TransactionEntity>()) }
        }

        @Test
        fun `rejeita alteracao que aponta remetente e destinatario para a mesma conta`() {
            val conta = Fixtures.account(id = 1L, balance = "70.00")
            val outra = Fixtures.account(id = 2L, balance = "80.00")
            val existente = Fixtures.transaction(conta, outra, id = 7L, amount = "30.00")
            val alteracao = Fixtures.transaction(conta, conta, id = null, amount = "10.00")
            every { transactionRepository.findById(7L) } returns Optional.of(existente)
            permitirSalvarContas()

            assertFailsWith<SameAccountTransferException> { service.update(7L, alteracao) }

            verify(exactly = 0) { transactionRepository.save(any<TransactionEntity>()) }
        }
    }

    @Nested
    inner class Delete {

        @Test
        fun `estorna a transferencia antes de remover`() {
            val sender = Fixtures.account(id = 1L, balance = "70.00")
            val receiver = Fixtures.account(id = 2L, balance = "80.00")
            val tx = Fixtures.transaction(sender, receiver, id = 7L, amount = "30.00")
            every { transactionRepository.findById(7L) } returns Optional.of(tx)
            permitirSalvarContas()
            every { transactionRepository.delete(tx) } returns Unit

            service.delete(7L)

            assertEquals(BigDecimal("100.00"), sender.balance)
            assertEquals(BigDecimal("50.00"), receiver.balance)
            verify(exactly = 1) { accountRepository.save(sender) }
            verify(exactly = 1) { accountRepository.save(receiver) }
            verify(exactly = 1) { transactionRepository.delete(tx) }
        }

        @Test
        fun `propaga TransactionNotFoundException e nao remove nada`() {
            every { transactionRepository.findById(7L) } returns Optional.empty()

            assertFailsWith<TransactionNotFoundException> { service.delete(7L) }

            verify(exactly = 0) { transactionRepository.delete(any<TransactionEntity>()) }
            verify(exactly = 0) { accountRepository.save(any<AccountEntity>()) }
        }

        @Test
        fun `criar e depois excluir devolve os saldos ao estado original`() {
            val sender = Fixtures.account(id = 1L, balance = "100.00")
            val receiver = Fixtures.account(id = 2L, balance = "50.00")
            val tx = Fixtures.transaction(sender, receiver, id = 7L, amount = "30.00")
            permitirSalvarContas()
            every { transactionRepository.save(tx) } returns tx
            every { transactionRepository.findById(7L) } returns Optional.of(tx)
            every { transactionRepository.delete(tx) } returns Unit

            service.create(tx)
            service.delete(7L)

            assertEquals(BigDecimal("100.00"), sender.balance)
            assertEquals(BigDecimal("50.00"), receiver.balance)
        }
    }
}
