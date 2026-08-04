package tregu_studing_plan.finance_api.Security

import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import tregu_studing_plan.finance_api.Security.UserRepository

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    // Usado no login: valida email + senha
    fun authenticate(email: String, senha: String): User? {
        val user = userRepository.findByEmail(email) ?: return null

        return if (passwordEncoder.matches(senha, user.password)) {
            user
        } else {
            null
        }
    }

    // Usado no /refresh: busca o usuário ATUAL pelo email do token
    fun findByEmail(email: String): User? {
        return userRepository.findByEmail(email)
    }

    // Usado no cadastro: nunca salva senha crua
    fun register(email: String, rawPassword: String): User {
        val hash = passwordEncoder.encode(rawPassword)
        val user = User(email, hash, emptyList())
        return userRepository.save(user)
    }
}