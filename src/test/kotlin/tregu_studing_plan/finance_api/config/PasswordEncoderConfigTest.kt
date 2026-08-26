package tregu_studing_plan.finance_api.config

import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PasswordEncoderConfigTest {

    private val encoder = DefiniteUsers().passwordEncoder()

    @Test
    fun `o bean e um BCryptPasswordEncoder`() {
        assertTrue(encoder is BCryptPasswordEncoder)
    }

    @Test
    fun `o hash nunca e a senha crua`() {
        assertNotEquals("senha-secreta", encoder.encode("senha-secreta"))
    }

    @Test
    fun `o hash tem o prefixo bcrypt`() {
        val hash = assertNotNull(encoder.encode("senha-secreta"))

        assertTrue(hash.startsWith("\$2a\$"), "hash gerado: $hash")
    }

    @Test
    fun `matches confirma a senha correta`() {
        assertTrue(encoder.matches("senha-secreta", encoder.encode("senha-secreta")))
    }

    @Test
    fun `matches recusa a senha errada`() {
        assertFalse(encoder.matches("senha-errada", encoder.encode("senha-secreta")))
    }

    @Test
    fun `mesma senha gera hashes diferentes - o salt e aleatorio`() {
        val primeiro = encoder.encode("senha-secreta")
        val segundo = encoder.encode("senha-secreta")

        assertNotEquals(primeiro, segundo)
        assertTrue(encoder.matches("senha-secreta", primeiro))
        assertTrue(encoder.matches("senha-secreta", segundo))
    }
}
