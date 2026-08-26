package tregu_studing_plan.finance_api.Security

import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JwtAuthFilterTest {

    private val jwtService = JwtService(
        secret = "chave-de-teste-com-pelo-menos-256-bits-de-comprimento-ok",
        acessExpiration = 900_000L,
        refreshExpiration = 604_800_000L,
    )
    private val filter = JwtAuthFilter(jwtService)
    private val chain = mockk<FilterChain>(relaxed = true)

    @BeforeEach
    @AfterEach
    fun limparContexto() = SecurityContextHolder.clearContext()

    private fun executar(header: String? = null): MockHttpServletResponse {
        val request = MockHttpServletRequest("GET", "/accounts")
        header?.let { request.addHeader("Authorization", it) }
        val response = MockHttpServletResponse()
        filter.doFilter(request, response, chain)
        return response
    }

    private val autenticacaoAtual get() = SecurityContextHolder.getContext().authentication

    @Test
    fun `sem header Authorization segue a cadeia sem autenticar`() {
        executar(header = null)

        assertNull(autenticacaoAtual, "rota publica decide depois; o filtro nao bloqueia")
        verify(exactly = 1) { chain.doFilter(any(), any()) }
    }

    @Test
    fun `header que nao e Bearer segue a cadeia sem autenticar`() {
        executar(header = "Basic YWxpY2U6c2VjcmV0")

        assertNull(autenticacaoAtual)
        verify(exactly = 1) { chain.doFilter(any(), any()) }
    }

    @Test
    fun `prefixo Bearer e sensivel a maiusculas`() {
        val token = jwtService.generateAccessToken("alice@tregu.dev", listOf("ROLE_USER"))

        executar(header = "bearer $token")

        assertNull(autenticacaoAtual, "so 'Bearer ' com B maiusculo e aceito")
        verify(exactly = 1) { chain.doFilter(any(), any()) }
    }

    @Test
    fun `token valido autentica com o email como principal`() {
        val token = jwtService.generateAccessToken("alice@tregu.dev", listOf("ROLE_USER"))

        executar(header = "Bearer $token")

        val auth = assertNotNull(autenticacaoAtual)
        assertEquals("alice@tregu.dev", auth.principal)
        assertTrue(auth.isAuthenticated)
    }

    @Test
    fun `token valido nunca carrega credenciais no contexto`() {
        val token = jwtService.generateAccessToken("alice@tregu.dev", listOf("ROLE_USER"))

        executar(header = "Bearer $token")

        assertNull(assertNotNull(autenticacaoAtual).credentials, "a senha nao circula apos o login")
    }

    @Test
    fun `token valido copia os papeis do token para as authorities`() {
        val token = jwtService.generateAccessToken("alice@tregu.dev", listOf("ROLE_USER", "ROLE_ADMIN"))

        executar(header = "Bearer $token")

        assertEquals(
            setOf("ROLE_USER", "ROLE_ADMIN"),
            assertNotNull(autenticacaoAtual).authorities.map { it.authority }.toSet(),
        )
    }

    @Test
    fun `token valido sem papeis autentica sem authorities`() {
        val token = jwtService.generateAccessToken("alice@tregu.dev", emptyList())

        executar(header = "Bearer $token")

        assertTrue(assertNotNull(autenticacaoAtual).authorities.isEmpty())
    }

    @Test
    fun `refresh token tambem autentica, porem sem papel algum`() {
        val refresh = jwtService.generateRefreshToken("alice@tregu.dev")

        executar(header = "Bearer $refresh")

        val auth = assertNotNull(autenticacaoAtual)
        assertEquals("alice@tregu.dev", auth.principal)
        assertTrue(auth.authorities.isEmpty())
    }

    @Test
    fun `token expirado nao autentica`() {
        val expirado = JwtService(
            secret = "chave-de-teste-com-pelo-menos-256-bits-de-comprimento-ok",
            acessExpiration = -1_000L,
            refreshExpiration = -1_000L,
        ).generateAccessToken("alice@tregu.dev", listOf("ROLE_ADMIN"))

        executar(header = "Bearer $expirado")

        assertNull(autenticacaoAtual)
        verify(exactly = 1) { chain.doFilter(any(), any()) }
    }

    @Test
    fun `token assinado com outra chave nao autentica`() {
        val intruso = JwtService(
            secret = "outra-chave-de-teste-com-pelo-menos-256-bits-diferente!!",
            acessExpiration = 900_000L,
            refreshExpiration = 604_800_000L,
        ).generateAccessToken("intruso@tregu.dev", listOf("ROLE_ADMIN"))

        executar(header = "Bearer $intruso")

        assertNull(autenticacaoAtual, "assinatura de outra chave nao pode virar sessao")
    }

    @Test
    fun `token ilegivel nao autentica e nao derruba a requisicao`() {
        executar(header = "Bearer nao-sou-um-token")

        assertNull(autenticacaoAtual)
        verify(exactly = 1) { chain.doFilter(any(), any()) }
    }

    @Test
    fun `header Bearer vazio nao autentica`() {
        executar(header = "Bearer ")

        assertNull(autenticacaoAtual)
        verify(exactly = 1) { chain.doFilter(any(), any()) }
    }
}
