package tregu_studing_plan.finance_api.Domain.Repository

import org.springframework.data.jpa.repository.JpaRepository
import tregu_studing_plan.finance_api.Domain.Entity.AccountEntity

interface AccountRepository : JpaRepository<AccountEntity, Long>
