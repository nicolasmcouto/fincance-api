package tregu_studing_plan.finance_api.Domain.Repository

import org.springframework.data.jpa.repository.JpaRepository
import tregu_studing_plan.finance_api.Domain.Entity.TransactionEntity

interface TransactionRepository : JpaRepository<TransactionEntity, Long>
