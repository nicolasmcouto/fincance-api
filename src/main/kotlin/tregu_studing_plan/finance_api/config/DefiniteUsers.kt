package tregu_studing_plan.finance_api.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class DefiniteUsers {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}