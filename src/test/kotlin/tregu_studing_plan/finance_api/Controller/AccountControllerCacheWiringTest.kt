package tregu_studing_plan.finance_api.Controller

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
 * Regressao do bug de cache que existia em `AccountController.create`/`update`:
 * as duas rotas carregavam `@CacheEvict(key = "#transation.account.id")`, um
 * parametro que nao existe no metodo. O SpEL resolvia para `null` e a chamada
 * estourava `SpelEvaluationException: EL1007E: Property or field 'account'
 * cannot be found on null` assim que o proxy de cache do `@EnableCaching`
 * interceptava o metodo — por isso o bug so aparecia com o contexto real de pe,
 * nunca nos testes unitarios de `AccountControllerTest` (que instanciam o
 * controller direto, sem proxy).
 *
 * Corrigido removendo as duas anotacoes do controller: a invalidacao do cache
 * "accounts" ja e feita corretamente em `AccountService` (@CachePut/@CacheEvict).
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
