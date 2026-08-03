package tregu_studing_plan.finance_api.Service

import tregu_studing_plan.finance_api.Domain.Entity.UserEntity
import tregu_studing_plan.finance_api.Domain.Repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    // Usado no login: valida email + senha
    fun authenticate(email: String, senha: String): UserEntity? {
        val user = userRepository.findByEmail(email) ?: return null

        return if (passwordEncoder.matches(senha, user.passwordHash)) {
            user
        } else {
            null
        }
    }

    // Usado no /refresh: busca o usuário ATUAL pelo email do token
    fun findByEmail(email: String): UserEntity? {
        return userRepository.findByEmail(email)
    }

    // Usado no cadastro: nunca salva senha crua
    fun register(email: String, senhaCrua: String): UserEntity {
        val hash = passwordEncoder.encode(senhaCrua)
        val user = UserEntity(email = email, passwordHash = hash)
        return userRepository.save(user)
    }
}