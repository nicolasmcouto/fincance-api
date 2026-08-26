package tregu_studing_plan.finance_api.support

import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule
import tregu_studing_plan.finance_api.Domain.Entity.AccountEntity
import tregu_studing_plan.finance_api.Domain.Entity.TransactionEntity
import tregu_studing_plan.finance_api.Domain.Enum.AccountType
import tregu_studing_plan.finance_api.Domain.Enum.TransactionType
import tregu_studing_plan.finance_api.Exception.GlobalExceptionHandler
import tregu_studing_plan.finance_api.Security.Role
import tregu_studing_plan.finance_api.Security.UserEntity
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Fábricas compartilhadas por toda a bateria de testes.
 *
 * Regras:
 *  - datas fixas (nunca `LocalDateTime.now()` numa asserção);
 *  - `BigDecimal` sempre construído a partir de `String`, para a escala ser previsível.
 */
object Fixtures {

    val FIXED_DATE: LocalDateTime = LocalDateTime.of(2026, 1, 15, 10, 30, 0)

    /** Mapper equivalente ao que o Boot registra em runtime (Jackson 3 + módulo Kotlin). */
    val jsonMapper: JsonMapper = JsonMapper.builder().addModule(kotlinModule()).build()

    fun account(
        id: Long? = 1L,
        ownerName: String = "Alice",
        type: AccountType = AccountType.CHECKING,
        balance: String = "100.00",
        createdAt: LocalDateTime = FIXED_DATE,
    ) = AccountEntity(
        ownerName = ownerName,
        type = type,
        balance = BigDecimal(balance),
        id = id,
        createdAt = createdAt,
    )

    fun transaction(
        sender: AccountEntity,
        receiver: AccountEntity,
        id: Long? = 10L,
        description: String = "Transferencia",
        amount: String = "30.00",
        type: TransactionType = TransactionType.EXPENSE,
        date: LocalDateTime = FIXED_DATE,
    ) = TransactionEntity(
        description = description,
        amount = BigDecimal(amount),
        type = type,
        date = date,
        sender = sender,
        receiver = receiver,
        id = id,
    )

    fun user(
        id: Long? = 1L,
        email: String = "alice@tregu.dev",
        passwordHash: String = "\$2a\$10\$hashfake",
        roles: Set<Role> = setOf(Role.USER),
    ) = UserEntity().apply {
        this.id = id
        this.email = email
        this.passwordHash = passwordHash
        this.roles.clear()
        this.roles.addAll(roles)
    }

    fun toJson(value: Any): String = jsonMapper.writeValueAsString(value)

    /**
     * MockMvc sem contexto Spring: só o controller, o advice global e o conversor JSON.
     * Mantém os testes de controller no domínio "unitário" (milissegundos, sem banco).
     */
    fun standaloneMvc(vararg controllers: Any): MockMvc =
        MockMvcBuilders.standaloneSetup(*controllers)
            .setControllerAdvice(GlobalExceptionHandler())
            .setMessageConverters(JacksonJsonHttpMessageConverter(jsonMapper))
            .build()
}
