package tregu_studing_plan.finance_api.Security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtService: JwtService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val header = request.getHeader("Authorization")

        // Sem header "Bearer" → segue sem autenticar (rota pública decide depois)
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val token = header.substring(7) // remove "Bearer "

        if (jwtService.isValid(token)) {
            val email = jwtService.extractEmail(token)
            // Aqui você poderia recarregar roles; simplificado:
            val auth = UsernamePasswordAuthenticationToken(
                email, null, listOf(SimpleGrantedAuthority("ROLE_USER"))
            )
            // Marca o usuário como autenticado no contexto do Spring
            SecurityContextHolder.getContext().authentication = auth
        }

        filterChain.doFilter(request, response)
    }
}