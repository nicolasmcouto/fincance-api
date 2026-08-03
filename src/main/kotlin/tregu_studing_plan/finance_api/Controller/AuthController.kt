package tregu_studing_plan.finance_api.Controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tregu_studing_plan.finance_api.Domain.Dto.RegisterRequest
import tregu_studing_plan.finance_api.Domain.Repository.UserRepository
import tregu_studing_plan.finance_api.Security.JwtService
import tregu_studing_plan.finance_api.Service.UserService

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
            val user = userService.
        }
}