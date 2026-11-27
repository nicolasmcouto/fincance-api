package tregu_studing_plan.finance_api.Controller

import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tregu_studing_plan.finance_api.Exception.GlobalExceptionHandler
import tregu_studing_plan.finance_api.Service.AccountService

class AccountControllerValidationTest {

    private val accountService = mockk<AccountService>()

    private val mvc = MockMvcBuilders
        .standaloneSetup(AccountController(accountService))
        .setControllerAdvice(GlobalExceptionHandler())
        .build()

    private fun postAccount(json: String) =
        mvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(json))
    
    @Test
    fun `ownerName vazio devolve 400`() {
        postAccount("""{"ownerName":"","type":"CHECKING","balance":10}""")
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.fields[0].field").value("ownerName"))
    }
}