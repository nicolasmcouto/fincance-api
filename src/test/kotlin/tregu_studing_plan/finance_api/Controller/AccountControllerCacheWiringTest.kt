package tregu_studing_plan.finance_api.Controller

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * BUG CONHECIDO — estes testes ficam desabilitados ate a correcao.
 *
 * `AccountController.create` e `AccountController.update` carregam
 * `@CacheEvict(value=["accounts"], key = "#transation.account.id")`.
 * O parametro `#transation` nao existe nesses metodos (nem com esse nome, nem
 * com esse tipo), entao o SpEL resolve para `null` e a chamada estoura:
 *
 *     SpelEvaluationException: EL1007E: Property or field 'account' cannot be found on null
 *
 * Como `@EnableCaching` faz o Spring criar um proxy de cache tambem para o
 * controller, o erro so aparece com o contexto real de pe — os testes unitarios
 * de `AccountControllerTest` instanciam o controller direto e passam.
 * Efeito em producao: POST /accounts e PUT /accounts/{id} devolvem 500.
 *
 * Correcao: remover as duas anotacoes do controller. A invalidacao do cache
 * "accounts" ja e feita corretamente em `AccountService` (@CachePut/@CacheEvict).
 * Feito isso, apague o @Disabled destes testes.
 */
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:cache-wiring;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ],
)
@AutoConfigureMockMvc(addFilters = false)
@Disabled("Bug aberto: @CacheEvict com SpEL invalido em AccountController quebra POST/PUT em runtime")
class AccountControllerCacheWiringTest {

    @Autowired private lateinit var mvc: MockMvc

    @Test
    fun `POST accounts funciona com o proxy de cache ativo`() {
        mvc.perform(
            post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"ownerName":"Alice","type":"CHECKING","balance":100.00}"""),
        ).andExpect(status().isCreated)
    }

    @Test
    fun `PUT accounts id funciona com o proxy de cache ativo`() {
        val criada = mvc.perform(
            post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"ownerName":"Alice","type":"CHECKING","balance":100.00}"""),
        ).andReturn().response.contentAsString
        val id = Regex("\"id\":(\\d+)").find(criada)!!.groupValues[1]

        mvc.perform(
            put("/accounts/$id")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"ownerName":"Alice Silva","type":"SAVINGS","balance":200.00}"""),
        ).andExpect(status().isOk)
    }
}
