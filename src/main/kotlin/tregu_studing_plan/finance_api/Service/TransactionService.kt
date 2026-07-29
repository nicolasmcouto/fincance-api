package tregu_studing_plan.finance_api.Service

import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tregu_studing_plan.finance_api.Domain.Entity.TransactionEntity
import tregu_studing_plan.finance_api.Exception.InsufficientBalanceException
import tregu_studing_plan.finance_api.Exception.SameAccountTransferException
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

    @Cacheable(value = ["accounts"], key = "#id")
    @Transactional(readOnly = true)
    fun findById(id: Long): TransactionEntity =
        transactionRepository.findById(id).orElseThrow { TransactionNotFoundException(id) }

    fun create(transaction: TransactionEntity): TransactionEntity {
        validateTransfer(transaction)
        transfer(transaction)
        return transactionRepository.save(transaction)
    }
    @CachePut(value = ["accounts"], key = "#result.id")
    fun update(id: Long, updated: TransactionEntity): TransactionEntity {
        val existing = findById(id)
        reverseTransfer(existing)

        existing.description = updated.description
        existing.amount = updated.amount
        existing.type = updated.type
        existing.date = updated.date
        existing.sender = updated.sender
        existing.receiver = updated.receiver

        validateTransfer(existing)
        transfer(existing)
        return transactionRepository.save(existing)
    }
    @CacheEvict
    fun delete(id: Long) {
        val transaction = findById(id)
        reverseTransfer(transaction)
        transactionRepository.delete(transaction)
    }

    private fun validateTransfer(transaction: TransactionEntity) {
        if (transaction.sender.id == transaction.receiver.id) {
            throw SameAccountTransferException()
        }
        if (transaction.sender.balance < transaction.amount) {
            throw InsufficientBalanceException(transaction.sender.id)
        }
    }

    private fun transfer(transaction: TransactionEntity) {
        transaction.sender.balance = transaction.sender.balance.subtract(transaction.amount)
        transaction.receiver.balance = transaction.receiver.balance.add(transaction.amount)
        accountRepository.save(transaction.sender)
        accountRepository.save(transaction.receiver)
    }

    private fun reverseTransfer(transaction: TransactionEntity) {
        transaction.sender.balance = transaction.sender.balance.add(transaction.amount)
        transaction.receiver.balance = transaction.receiver.balance.subtract(transaction.amount)
        accountRepository.save(transaction.sender)
        accountRepository.save(transaction.receiver)
    }
}
