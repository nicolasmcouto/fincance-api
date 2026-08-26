package tregu_studing_plan.finance_api.Security

import org.junit.jupiter.api.Test
import tregu_studing_plan.finance_api.support.Fixtures
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AppUserDetailsTest {

    @Test
    fun `username e o email do usuario`() {
        val details = AppUserDetails(Fixtures.user(email = "alice@tregu.dev"))

        assertEquals("alice@tregu.dev", details.username)
    }

    @Test
    fun `password e o hash armazenado, nunca a senha crua`() {
        val details = AppUserDetails(Fixtures.user(passwordHash = "\$2a\$10\$abc"))

        assertEquals("\$2a\$10\$abc", details.password)
    }

    @Test
    fun `id expoe a chave primaria do usuario`() {
        assertEquals(42L, AppUserDetails(Fixtures.user(id = 42L)).id)
    }

    @Test
    fun `id estoura quando o usuario ainda nao foi persistido`() {
        assertFailsWith<NullPointerException> { AppUserDetails(Fixtures.user(id = null)).id }
    }

    @Test
    fun `converte cada papel em uma authority com prefixo ROLE_`() {
        val details = AppUserDetails(Fixtures.user(roles = setOf(Role.USER, Role.ADMIN)))

        assertEquals(
            setOf("ROLE_USER", "ROLE_ADMIN"),
            details.authorities.map { it.authority }.toSet(),
        )
    }

    @Test
    fun `usuario com um unico papel tem uma unica authority`() {
        val details = AppUserDetails(Fixtures.user(roles = setOf(Role.MANAGER)))

        assertEquals(listOf("ROLE_MANAGER"), details.authorities.map { it.authority })
    }

    @Test
    fun `usuario sem papel nao tem authority`() {
        val details = AppUserDetails(Fixtures.user(roles = emptySet()))

        assertTrue(details.authorities.isEmpty())
    }

    @Test
    fun `a conta nunca esta expirada, travada ou desabilitada`() {
        val details = AppUserDetails(Fixtures.user())

        assertTrue(details.isAccountNonExpired)
        assertTrue(details.isAccountNonLocked)
        assertTrue(details.isCredentialsNonExpired)
        assertTrue(details.isEnabled)
    }
}
