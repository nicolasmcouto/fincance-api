package tregu_studing_plan.finance_api.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.web.SecurityFilterChain
import org.springframework.http.HttpMethod

@Configuration
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
            formLogin { disable() }

            authorizeHttpRequests {
                authorize(HttpMethod.POST, "/auth/registrar", permitAll)
                authorize("/actuator/health", permitAll)

                authorize(anyRequest, authenticated)
            }
        }
        return http.build()
    }
}
