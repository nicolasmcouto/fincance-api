package tregu_studing_plan.finance_api.Service

import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tregu_studing_plan.finance_api.Domain.Entity.AccountEntity
import tregu_studing_plan.finance_api.Exception.AccountNotFoundException
import tregu_studing_plan.finance_api.Domain.Repository.AccountRepository

@Service
@Transactional
class AccountService(
    private val accountRepository: AccountRepository,
) {

    @Transactional(readOnly = true)
    fun findAll(): List<AccountEntity> = accountRepository.findAll()

    @Cacheable(value=["accounts"], key = "#id")
    @Transactional(readOnly = true)
    fun findById(id: Long): AccountEntity =
        accountRepository.findById(id).orElseThrow { AccountNotFoundException(id) }

    fun create(account: AccountEntity): AccountEntity = accountRepository.save(account)

    @CachePut(value=["accounts"], key = "#result.id")
    fun update(id: Long, updated: AccountEntity): AccountEntity {
        val account = findById(id)
        account.ownerName = updated.ownerName
        account.type = updated.type
        account.balance = updated.balance
        return accountRepository.save(account)
    }

    @CacheEvict(value=["accounts"], key = "#id")
    fun delete(id: Long) {
        val account = findById(id)
        accountRepository.delete(account)
    }
}
