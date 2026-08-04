package tregu_studing_plan.finance_api.Security

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.security.core.userdetails.User

interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
}