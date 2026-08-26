package tregu_studing_plan.finance_api.Domain.Repository

import org.springframework.data.jpa.repository.JpaRepository
import tregu_studing_plan.finance_api.Domain.Entity.UserEntity

interface UserRepository : JpaRepository<UserEntity, Long> {
    fun findByEmail(email: String): UserEntity?
}