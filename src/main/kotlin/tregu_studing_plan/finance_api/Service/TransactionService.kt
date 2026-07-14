package tregu_studing_plan.finance_api.Service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tregu_studing_plan.finance_api.Domain.Entity.TransactionEntity
import tregu_studing_plan.finance_api.Domain.Enum.TransactionType
import tregu_studing_plan.finance_api.Exception.TransactionNotFoundException
import tregu_studing_plan.finance_api.Domain.Repository.AccountRepository
import tregu_studing_plan.finance_api.Domain.Repository.TransactionRepository

@Service
@Transactional
class TransactionService(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
) {

    @Transactional(readOnly = true)
    fun findAll(): List<TransactionEntity> = transactionRepository.findAll()

    @Transactional(readOnly = true)
    fun findById(id: Long): TransactionEntity =
        transactionRepository.findById(id).orElseThrow { TransactionNotFoundException(id) }

    fun create(transaction: TransactionEntity): TransactionEntity {
        applyToBalance(transaction, reverse = false)
        return transactionRepository.save(transaction)
    }

    fun update(id: Long, updated: TransactionEntity): TransactionEntity {
        val existing = findById(id)
        applyToBalance(existing, reverse = true)

        existing.description = updated.description
        existing.amount = updated.amount
        existing.type = updated.type
        existing.date = updated.date
        existing.account = updated.account

        applyToBalance(existing, reverse = false)
        return transactionRepository.save(existing)
    }

    fun delete(id: Long) {
        val transaction = findById(id)
        applyToBalance(transaction, reverse = true)
        transactionRepository.delete(transaction)
    }

    /** Aplica (ou reverte) o efeito da transação no saldo da conta. */
    private fun applyToBalance(transaction: TransactionEntity, reverse: Boolean) {
        val account = transaction.account
        val signedAmount = if (transaction.type == TransactionType.INCOME) transaction.amount else transaction.amount.negate()
        account.balance = if (reverse) account.balance.subtract(signedAmount) else account.balance.add(signedAmount)
        accountRepository.save(account)
    }
}
