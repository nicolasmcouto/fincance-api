package tregu_studing_plan.finance_api.Security

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tregu_studing_plan.finance_api.support.Fixtures

class AuthControllerTest {

    private val jwtService = mockk<JwtService>()
    private val userService = mockk<UserService>()
    private val mvc = Fixtures.standaloneMvc(AuthController(jwtService, userService))

    private val alice = AppUserDetails(Fixtures.user(email = "alice@tregu.dev", roles = setOf(Role.USER, Role.ADMIN)))

    @Nested
    inner class Login {

        @Test
        fun `credenciais validas devolvem 200 com access e refresh token`() {
            every { userService.authenticate("alice@tregu.dev", "senha-certa") } returns alice
            every { jwtService.generateAccessToken("alice@tregu.dev", any()) } returns "access-token"
            every { jwtService.generateRefreshToken("alice@tregu.dev") } returns "refresh-token"

            mvc.perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(Fixtures.toJson(LoginRequest("alice@tregu.dev", "senha-certa"))),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
        }

        @Test
        fun `o access token carrega os papeis do usuario`() {
            every { userService.authenticate(any(), any()) } returns alice
            every { jwtService.generateAccessToken(any(), any()) } returns "access-token"
            every { jwtService.generateRefreshToken(any()) } returns "refresh-token"

            mvc.perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(Fixtures.toJson(LoginRequest("alice@tregu.dev", "senha-certa"))),
            ).andExpect(status().isOk)

            verify {
                jwtService.generateAccessToken(
                    "alice@tregu.dev",
                    match { it.toSet() == setOf("ROLE_USER", "ROLE_ADMIN") },
                )
            }
        }

        @Test
        fun `senha errada devolve 401 sem corpo`() {
            every { userService.authenticate("alice@tregu.dev", "senha-errada") } returns null

            mvc.perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(Fixtures.toJson(LoginRequest("alice@tregu.dev", "senha-errada"))),
            )
                .andExpect(status().isUnauthorized)
                .andExpect(content().string(""))
        }

        @Test
        fun `email desconhecido devolve 401 e nao emite token`() {
            every { userService.authenticate("fantasma@tregu.dev", any()) } returns null

            mvc.perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(Fixtures.toJson(LoginRequest("fantasma@tregu.dev", "qualquer"))),
            ).andExpect(status().isUnauthorized)

            verify(exactly = 0) { jwtService.generateAccessToken(any(), any()) }
            verify(exactly = 0) { jwtService.generateRefreshToken(any()) }
        }
    }

    @Nested
    inner class Refresh {

        @Test
        fun `refresh valido devolve 200 com um novo access token`() {
            every { jwtService.isValid("refresh-valido") } returns true
            every { jwtService.extractEmail("refresh-valido") } returns "alice@tregu.dev"
            every { userService.findByEmail("alice@tregu.dev") } returns alice
            every { jwtService.generateAccessToken("alice@tregu.dev", any()) } returns "novo-access-token"

            mvc.perform(
                post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(Fixtures.toJson(RefreshRequest("refresh-valido"))),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.accessToken").value("novo-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-valido"))
        }

        @Test
        fun `refresh e reaproveitado - nao ha rotacao`() {
            every { jwtService.isValid(any()) } returns true
            every { jwtService.extractEmail(any()) } returns "alice@tregu.dev"
            every { userService.findByEmail(any()) } returns alice
            every { jwtService.generateAccessToken(any(), any()) } returns "novo-access-token"

            mvc.perform(
                post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(Fixtures.toJson(RefreshRequest("refresh-valido"))),
            ).andExpect(jsonPath("$.refreshToken").value("refresh-valido"))

            verify(exactly = 0) { jwtService.generateRefreshToken(any()) }
        }

        @Test
        fun `os papeis sao relidos do banco, nao do token`() {
            every { jwtService.isValid(any()) } returns true
            every { jwtService.extractEmail(any()) } returns "alice@tregu.dev"
            every { userService.findByEmail("alice@tregu.dev") } returns
                AppUserDetails(Fixtures.user(email = "alice@tregu.dev", roles = setOf(Role.MANAGER)))
            every { jwtService.generateAccessToken(any(), any()) } returns "novo-access-token"

            mvc.perform(
                post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(Fixtures.toJson(RefreshRequest("refresh-valido"))),
            ).andExpect(status().isOk)

            verify { jwtService.generateAccessToken("alice@tregu.dev", listOf("ROLE_MANAGER")) }
            verify(exactly = 0) { jwtService.extractRoles(any()) }
        }

        @Test
        fun `refresh invalido devolve 401 e nem consulta o usuario`() {
            every { jwtService.isValid("refresh-podre") } returns false

            mvc.perform(
                post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(Fixtures.toJson(RefreshRequest("refresh-podre"))),
            )
                .andExpect(status().isUnauthorized)
                .andExpect(content().string(""))

            verify(exactly = 0) { userService.findByEmail(any()) }
            verify(exactly = 0) { jwtService.generateAccessToken(any(), any()) }
        }

        @Test
        fun `refresh valido de usuario ja removido devolve 401`() {
            every { jwtService.isValid("refresh-valido") } returns true
            every { jwtService.extractEmail("refresh-valido") } returns "removido@tregu.dev"
            every { userService.findByEmail("removido@tregu.dev") } returns null

            mvc.perform(
                post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(Fixtures.toJson(RefreshRequest("refresh-valido"))),
            ).andExpect(status().isUnauthorized)

            verify(exactly = 0) { jwtService.generateAccessToken(any(), any()) }
        }
    }

    @Nested
    inner class Registrar {

        @Test
        fun `cadastro devolve 201 sem corpo`() {
            every { userService.register("nova@tregu.dev", "senha-nova") } returns
                AppUserDetails(Fixtures.user(id = 2L, email = "nova@tregu.dev"))

            mvc.perform(
                post("/auth/registrar")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(Fixtures.toJson(RegisterRequest("nova@tregu.dev", "senha-nova"))),
            )
                .andExpect(status().isCreated)
                .andExpect(content().string(""))
        }

        @Test
        fun `cadastro nao devolve token - o cliente precisa fazer login`() {
            every { userService.register(any(), any()) } returns
                AppUserDetails(Fixtures.user(id = 2L, email = "nova@tregu.dev"))

            mvc.perform(
                post("/auth/registrar")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(Fixtures.toJson(RegisterRequest("nova@tregu.dev", "senha-nova"))),
            ).andExpect(status().isCreated)

            verify(exactly = 0) { jwtService.generateAccessToken(any(), any()) }
            verify(exactly = 0) { jwtService.generateRefreshToken(any()) }
        }

        @Test
        fun `cadastro encaminha email e senha crua para o service`() {
            every { userService.register(any(), any()) } returns
                AppUserDetails(Fixtures.user(id = 2L, email = "nova@tregu.dev"))

            mvc.perform(
                post("/auth/registrar")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(Fixtures.toJson(RegisterRequest("nova@tregu.dev", "senha-nova"))),
            ).andExpect(status().isCreated)

            verify(exactly = 1) { userService.register("nova@tregu.dev", "senha-nova") }
        }
    }

    @Nested
    inner class ContratoDosDtos {

        @Test
        fun `login usa o campo senha em portugues`() {
            every { userService.authenticate("alice@tregu.dev", "senha-certa") } returns alice
            every { jwtService.generateAccessToken(any(), any()) } returns "access-token"
            every { jwtService.generateRefreshToken(any()) } returns "refresh-token"

            mvc.perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email":"alice@tregu.dev","senha":"senha-certa"}"""),
            ).andExpect(status().isOk)
        }

        @Test
        fun `cadastro usa o campo password em ingles`() {
            every { userService.register("nova@tregu.dev", "senha-nova") } returns
                AppUserDetails(Fixtures.user(id = 2L, email = "nova@tregu.dev"))

            mvc.perform(
                post("/auth/registrar")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email":"nova@tregu.dev","password":"senha-nova"}"""),
            ).andExpect(status().isCreated)
        }
    }
}
