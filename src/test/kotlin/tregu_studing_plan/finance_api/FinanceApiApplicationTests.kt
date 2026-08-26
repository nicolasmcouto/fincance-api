package tregu_studing_plan.finance_api

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.security.web.SecurityFilterChain
import tregu_studing_plan.finance_api.Controller.AccountController
import tregu_studing_plan.finance_api.Controller.TransactionController
import tregu_studing_plan.finance_api.Security.AuthController
import tregu_studing_plan.finance_api.Security.JwtAuthFilter
import tregu_studing_plan.finance_api.Security.JwtService
import tregu_studing_plan.finance_api.Service.AccountService
import tregu_studing_plan.finance_api.Service.TransactionService
import kotlin.test.assertNotNull

/**
 * Smoke test da fiacao completa do Spring.
 *
 * O banco real (MySQL) e substituido por H2 em memoria e o Flyway fica desligado:
 * as migrations usam sintaxe especifica de MySQL. O objetivo aqui e provar que o
 * contexto sobe — beans, seguranca e cache — sem exigir infraestrutura local.
 */
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:finance;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ],
)
class FinanceApiApplicationTests {

    @Autowired private lateinit var accountController: AccountController
    @Autowired private lateinit var transactionController: TransactionController
    @Autowired private lateinit var authController: AuthController
    @Autowired private lateinit var accountService: AccountService
    @Autowired private lateinit var transactionService: TransactionService
    @Autowired private lateinit var jwtService: JwtService
    @Autowired private lateinit var jwtAuthFilter: JwtAuthFilter
    @Autowired private lateinit var securityFilterChain: SecurityFilterChain
    @Autowired private lateinit var cacheManager: CacheManager

    @Test
    fun contextLoads() {
        assertNotNull(accountController)
        assertNotNull(transactionController)
        assertNotNull(authController)
        assertNotNull(accountService)
        assertNotNull(transactionService)
    }

    @Test
    fun `a cadeia de seguranca e o filtro JWT estao registrados`() {
        assertNotNull(securityFilterChain)
        assertNotNull(jwtAuthFilter)
        assertNotNull(jwtService)
    }

    @Test
    fun `o cache esta habilitado`() {
        assertNotNull(cacheManager)
    }
}
