package tregu_studing_plan.finance_api.Security

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class LoginRequest(val email: String, val senha: String)
data class TokenResponse(val accessToken: String, val refreshToken: String)
data class RefreshRequest(val refreshToken: String)

@RestController
@RequestMapping("/auth")
class AuthController(
    private val jwtService: JwtService,
    private val userService: UserService
){
    @PostMapping("/login")
        fun login(@RequestBody req : LoginRequest): ResponseEntity<TokenResponse> {

        val user = userService.authenticate(req.email, req.senha) ?: return ResponseEntity.status(401).build()

        val acess = jwtService.generateAccessToken(user.username, user.authorities.mapNotNull { it.authority })
        val refresh = jwtService.generateRefreshToken(user.username)

        return ResponseEntity.ok(TokenResponse(acess, refresh))
    }

    @PostMapping("/refresh")
    fun refresh(@RequestBody req: RefreshRequest): ResponseEntity<TokenResponse> {
        // 1. Refresh token é válido?
        if (!jwtService.isValid(req.refreshToken)) {
            return ResponseEntity.status(401).build()
        }

        // 2. Descobre quem é e gera um NOVO access token
        val email = jwtService.extractEmail(req.refreshToken)
        val user = userService.findByEmail(email)
            ?: return ResponseEntity.status(401).build()

        val newAccess = jwtService.generateAccessToken(user.username, user.authorities.mapNotNull { it.authority })

        // Reaproveita o refresh (ou gere um novo, se quiser rotação)
        return ResponseEntity.ok(TokenResponse(newAccess, req.refreshToken))
    }

    @PostMapping("/registrar")
    fun registrar(@RequestBody req: RegisterRequest): ResponseEntity<Void> {
        userService.register(req.email, req.password)
        return ResponseEntity.status(201).build()
    }
}