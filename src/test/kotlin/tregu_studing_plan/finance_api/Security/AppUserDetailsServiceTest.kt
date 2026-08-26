package tregu_studing_plan.finance_api.Security

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.security.core.userdetails.UsernameNotFoundException
import tregu_studing_plan.finance_api.support.Fixtures
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AppUserDetailsServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val service = AppUserDetailsService(userRepository)

    @Test
    fun `carrega o usuario pelo email`() {
        every { userRepository.findByEmail("alice@tregu.dev") } returns
            Fixtures.user(email = "alice@tregu.dev", roles = setOf(Role.ADMIN))

        val details = service.loadUserByUsername("alice@tregu.dev")

        assertEquals("alice@tregu.dev", details.username)
        assertEquals(listOf("ROLE_ADMIN"), details.authorities.map { it.authority })
    }

    @Test
    fun `lanca UsernameNotFoundException quando o email e desconhecido`() {
        every { userRepository.findByEmail("fantasma@tregu.dev") } returns null

        val ex = assertFailsWith<UsernameNotFoundException> { service.loadUserByUsername("fantasma@tregu.dev") }
        assertEquals("Usuário não encontrado: fantasma@tregu.dev", ex.message)
    }

    @Test
    fun `consulta o repositorio uma unica vez`() {
        every { userRepository.findByEmail(any()) } returns Fixtures.user()

        service.loadUserByUsername("alice@tregu.dev")

        verify(exactly = 1) { userRepository.findByEmail("alice@tregu.dev") }
    }
}
