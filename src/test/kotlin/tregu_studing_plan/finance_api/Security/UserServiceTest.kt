package tregu_studing_plan.finance_api.Security

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder
import tregu_studing_plan.finance_api.support.Fixtures
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UserServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val service = UserService(userRepository, passwordEncoder)

    @Nested
    inner class Authenticate {

        @Test
        fun `devolve os detalhes quando a senha confere`() {
            val user = Fixtures.user(email = "alice@tregu.dev", passwordHash = "hash", roles = setOf(Role.ADMIN))
            every { userRepository.findByEmail("alice@tregu.dev") } returns user
            every { passwordEncoder.matches("senha-certa", "hash") } returns true

            val details = assertNotNull(service.authenticate("alice@tregu.dev", "senha-certa"))

            assertEquals("alice@tregu.dev", details.username)
            assertEquals(listOf("ROLE_ADMIN"), details.authorities.map { it.authority })
        }

        @Test
        fun `devolve null quando a senha esta errada`() {
            val user = Fixtures.user(passwordHash = "hash")
            every { userRepository.findByEmail("alice@tregu.dev") } returns user
            every { passwordEncoder.matches("senha-errada", "hash") } returns false

            assertNull(service.authenticate("alice@tregu.dev", "senha-errada"))
        }

        @Test
        fun `devolve null quando o email nao existe`() {
            every { userRepository.findByEmail("fantasma@tregu.dev") } returns null

            assertNull(service.authenticate("fantasma@tregu.dev", "qualquer"))
        }

        @Test
        fun `nao gasta ciclos de hash quando o email nao existe`() {
            every { userRepository.findByEmail("fantasma@tregu.dev") } returns null

            service.authenticate("fantasma@tregu.dev", "qualquer")

            verify(exactly = 0) { passwordEncoder.matches(any(), any()) }
        }

        @Test
        fun `compara a senha crua contra o hash armazenado`() {
            val user = Fixtures.user(passwordHash = "hash-do-banco")
            every { userRepository.findByEmail(any()) } returns user
            every { passwordEncoder.matches(any(), any()) } returns true

            service.authenticate("alice@tregu.dev", "senha-crua")

            verify(exactly = 1) { passwordEncoder.matches("senha-crua", "hash-do-banco") }
        }
    }

    @Nested
    inner class FindByEmail {

        @Test
        fun `devolve os detalhes do usuario atual`() {
            every { userRepository.findByEmail("alice@tregu.dev") } returns
                Fixtures.user(email = "alice@tregu.dev", roles = setOf(Role.MANAGER))

            val details = assertNotNull(service.findByEmail("alice@tregu.dev"))

            assertEquals("alice@tregu.dev", details.username)
            assertEquals(listOf("ROLE_MANAGER"), details.authorities.map { it.authority })
        }

        @Test
        fun `devolve null quando o usuario nao existe mais`() {
            every { userRepository.findByEmail("removido@tregu.dev") } returns null

            assertNull(service.findByEmail("removido@tregu.dev"))
        }

        @Test
        fun `nao valida senha - o token ja foi verificado`() {
            every { userRepository.findByEmail(any()) } returns Fixtures.user()

            service.findByEmail("alice@tregu.dev")

            verify(exactly = 0) { passwordEncoder.matches(any(), any()) }
        }
    }

    @Nested
    inner class Register {

        @Test
        fun `nunca persiste a senha crua`() {
            val salvo = slot<UserEntity>()
            every { passwordEncoder.encode("senha-crua") } returns "hash-bcrypt"
            every { userRepository.save(capture(salvo)) } answers { Fixtures.user(id = 1L) }

            service.register("alice@tregu.dev", "senha-crua")

            assertEquals("hash-bcrypt", salvo.captured.passwordHash)
            assertNotEquals("senha-crua", salvo.captured.passwordHash)
        }

        @Test
        fun `persiste o email informado`() {
            val salvo = slot<UserEntity>()
            every { passwordEncoder.encode(any()) } returns "hash-bcrypt"
            every { userRepository.save(capture(salvo)) } answers { Fixtures.user(id = 1L) }

            service.register("alice@tregu.dev", "senha-crua")

            assertEquals("alice@tregu.dev", salvo.captured.email)
        }

        @Test
        fun `o novo usuario nasce com o papel USER`() {
            val salvo = slot<UserEntity>()
            every { passwordEncoder.encode(any()) } returns "hash-bcrypt"
            every { userRepository.save(capture(salvo)) } answers { Fixtures.user(id = 1L) }

            service.register("alice@tregu.dev", "senha-crua")

            assertEquals(setOf(Role.USER), salvo.captured.roles, "cadastro publico nunca concede ADMIN")
        }

        @Test
        fun `devolve os detalhes do usuario ja persistido`() {
            every { passwordEncoder.encode(any()) } returns "hash-bcrypt"
            every { userRepository.save(any()) } returns
                Fixtures.user(id = 99L, email = "alice@tregu.dev", passwordHash = "hash-bcrypt")

            val details = service.register("alice@tregu.dev", "senha-crua")

            assertEquals(99L, details.id)
            assertEquals("alice@tregu.dev", details.username)
            assertEquals("hash-bcrypt", details.password)
        }

        @Test
        fun `chama o encoder exatamente uma vez`() {
            every { passwordEncoder.encode(any()) } returns "hash-bcrypt"
            every { userRepository.save(any()) } returns Fixtures.user(id = 1L)

            service.register("alice@tregu.dev", "senha-crua")

            verify(exactly = 1) { passwordEncoder.encode("senha-crua") }
            verify(exactly = 1) { userRepository.save(any()) }
        }
    }
}
