package tregu_studing_plan.finance_api.Service

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey

enum class TokenType { ACCESS, REFRESH }

@Service
class JwtService(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.access-expiration}") private val acessExpiration: Long,
    @Value("\${jwt.refresh-expiration}") private val refreshExpiration: Long,
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

    fun generateAccessToken(email: String, roles: List<String>): String {
        return Jwts.builder()
            .subject(email)
            .claim("roles", roles)
            .claim(TYPE_CLAIM, TokenType.ACCESS.name)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + acessExpiration))
            .signWith(key)
            .compact()
    }
    fun generateRefreshToken(email: String): String {
        return Jwts.builder()
            .subject(email)
            .claim(TYPE_CLAIM, TokenType.REFRESH.name)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + refreshExpiration))
            .signWith(key)
            .compact()
    }
    fun extractEmail(email: String): String {
        return parseClaims(email).subject
    }

    fun extractRoles(token: String): List<String> {
        @Suppress("UNCHECKED_CAST")
        return parseClaims(token)["roles"] as? List<String> ?: emptyList()
    }

    private fun parseClaims(token: String) =
        Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload

    fun isValid(token: String, expectedType: TokenType): Boolean {
        return try {
            val claims = parseClaims(token)
            val naoExpirou = claims.expiration.after(Date())
            val tipoCerto = claims[TYPE_CLAIM] == expectedType.name
            naoExpirou && tipoCerto
        } catch (e: Exception) {
            false
        }
    }

    private companion object {
        const val TYPE_CLAIM = "type"
    }
}