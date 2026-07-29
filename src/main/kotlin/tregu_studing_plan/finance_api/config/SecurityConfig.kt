package tregu_studing_plan.finance_api.config

import org.apache.catalina.filters.HttpHeaderSecurityFilter
import org.springframework.context.annotation.Configuration

@Configuration
class SecurityConfig {

    fun securityFilterChain(http: HttpHeaderSecurityFilter): HttpHeaderSecurityFilter {

                http{

                    csrf()
                }
    }
}