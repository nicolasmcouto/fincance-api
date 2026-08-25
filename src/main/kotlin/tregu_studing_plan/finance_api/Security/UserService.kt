package tregu_studing_plan.finance_api.Security

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    // Usado no login: valida email + senha
    fun authenticate(email: String, senha: String): AppUserDetails? {
        val user = userRepository.findByEmail(email) ?: return null

        return if (passwordEncoder.matches(senha, user.passwordHash)) {
            AppUserDetails(user)
        } else {
            null
        }
    }

    // Usado no /refresh: busca o usuário ATUAL pelo email do token
    fun findByEmail(email: String): AppUserDetails? {
        return userRepository.findByEmail(email)?.let { AppUserDetails(it) }
    }

    // Usado no cadastro: nunca salva senha crua
    fun register(email: String, rawPassword: String): AppUserDetails {
        val user = UserEntity()
        user.email = email
        user.passwordHash = passwordEncoder.encode(rawPassword)
        val saved = userRepository.save(user)
        return AppUserDetails(saved)
    }
}