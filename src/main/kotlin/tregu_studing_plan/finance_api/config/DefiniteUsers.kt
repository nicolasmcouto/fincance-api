package tregu_studing_plan.finance_api.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager

// excluir classe antes do deploy
@Configuration
class DefiniteUsers {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun userDetailsService(encoder: PasswordEncoder): UserDetailsService {
            val nicolas = User.builder()
                .username("Nicolas")
                .password(encoder.encode("root"))
                .roles("USER")
                .build()

        return InMemoryUserDetailsManager(nicolas)
    }
}