package tregu_studing_plan.finance_api.Security

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class JwtServiceTest {

    private companion object {
        /** HS256 exige >= 256 bits; abaixo disso o jjwt recusa a chave. */
        const val SECRET = "chave-de-teste-com-pelo-menos-256-bits-de-comprimento-ok"
        const val OUTRO_SECRET = "outra-chave-de-teste-com-pelo-menos-256-bits-diferente!!"
        const val QUINZE_MINUTOS = 900_000L
        const val SETE_DIAS = 604_800_000L
    }

    private fun jwtService(
        secret: String = SECRET,
        accessExpiration: Long = QUINZE_MINUTOS,
        refreshExpiration: Long = SETE_DIAS,
    ) = JwtService(secret, accessExpiration, refreshExpiration)

    private val service = jwtService()

    @Nested
    inner class AccessToken {

        @Test
        fun `carrega o email no subject`() {
            val token = service.generateAccessToken("alice@tregu.dev", listOf("ROLE_USER"))

            assertEquals("alice@tregu.dev", service.extractEmail(token))
        }

        @Test
        fun `carrega os papeis na claim roles`() {
            val token = service.generateAccessToken("alice@tregu.dev", listOf("ROLE_USER", "ROLE_ADMIN"))

            assertEquals(listOf("ROLE_USER", "ROLE_ADMIN"), service.extractRoles(token))
        }

        @Test
        fun `aceita usuario sem papel algum`() {
            val token = service.generateAccessToken("alice@tregu.dev", emptyList())

            assertEquals(emptyList(), service.extractRoles(token))
            assertEquals("alice@tregu.dev", service.extractEmail(token))
        }

        @Test
        fun `nasce valido`() {
            assertTrue(service.isValid(service.generateAccessToken("alice@tregu.dev", listOf("ROLE_USER"))))
        }

        @Test
        fun `e um JWT compacto de tres segmentos`() {
            val token = service.generateAccessToken("alice@tregu.dev", listOf("ROLE_USER"))

            assertEquals(3, token.split(".").size, "header.payload.signature")
        }

        @Test
        fun `expira conforme jwt access-expiration`() {
            val expirado = jwtService(accessExpiration = -1_000L)
                .generateAccessToken("alice@tregu.dev", listOf("ROLE_USER"))

            assertFalse(service.isValid(expirado), "token emitido no passado nao pode ser aceito")
        }
    }

    @Nested
    inner class RefreshToken {

        @Test
        fun `carrega o email no subject`() {
            val token = service.generateRefreshToken("alice@tregu.dev")

            assertEquals("alice@tregu.dev", service.extractEmail(token))
        }

        @Test
        fun `nao carrega papeis - refresh so identifica o usuario`() {
            val token = service.generateRefreshToken("alice@tregu.dev")

            assertEquals(emptyList(), service.extractRoles(token))
        }

        @Test
        fun `nasce valido`() {
            assertTrue(service.isValid(service.generateRefreshToken("alice@tregu.dev")))
        }

        @Test
        fun `expira conforme jwt refresh-expiration`() {
            val expirado = jwtService(refreshExpiration = -1_000L).generateRefreshToken("alice@tregu.dev")

            assertFalse(service.isValid(expirado))
        }

        @Test
        fun `e diferente do access token do mesmo usuario`() {
            val access = service.generateAccessToken("alice@tregu.dev", listOf("ROLE_USER"))
            val refresh = service.generateRefreshToken("alice@tregu.dev")

            assertNotEquals(access, refresh)
        }
    }

    @Nested
    inner class Validacao {

        @Test
        fun `recusa string vazia`() {
            assertFalse(service.isValid(""))
        }

        @Test
        fun `recusa texto que nao e JWT`() {
            assertFalse(service.isValid("nao-sou-um-token"))
        }

        @Test
        fun `recusa JWT com estrutura valida mas conteudo lixo`() {
            assertFalse(service.isValid("aaa.bbb.ccc"))
        }

        @Test
        fun `recusa token assinado com outra chave`() {
            val intruso = jwtService(secret = OUTRO_SECRET).generateAccessToken("alice@tregu.dev", listOf("ROLE_ADMIN"))

            assertFalse(service.isValid(intruso), "assinatura de outra chave nao pode passar")
        }

        @Test
        fun `recusa token com assinatura adulterada`() {
            val token = service.generateAccessToken("alice@tregu.dev", listOf("ROLE_USER"))
            val ultimo = token.last()
            val adulterado = token.dropLast(1) + if (ultimo == 'A') 'B' else 'A'

            assertFalse(service.isValid(adulterado))
        }

        @Test
        fun `recusa token com payload adulterado - escalada de papel`() {
            val token = service.generateAccessToken("alice@tregu.dev", listOf("ROLE_USER"))
            val (header, payload, assinatura) = token.split(".")
            val payloadForjado = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                String(java.util.Base64.getUrlDecoder().decode(payload))
                    .replace("ROLE_USER", "ROLE_ADMIN")
                    .toByteArray(),
            )

            assertFalse(service.isValid("$header.$payloadForjado.$assinatura"))
        }

        @Test
        fun `extractEmail estoura em token invalido - isValid deve vir antes`() {
            val erro = runCatching { service.extractEmail("nao-sou-um-token") }

            assertTrue(erro.isFailure)
        }

        @Test
        fun `extractRoles estoura em token invalido`() {
            val erro = runCatching { service.extractRoles("nao-sou-um-token") }

            assertTrue(erro.isFailure)
        }
    }
}
