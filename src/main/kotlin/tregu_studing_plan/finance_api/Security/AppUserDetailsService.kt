package tregu_studing_plan.finance_api.Security

import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class AppUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): AppUserDetails {
        val user = userRepository.findByEmail(username)
            ?: throw UsernameNotFoundException("Usuário não encontrado: $username")

        return AppUserDetails(user)
    }
}