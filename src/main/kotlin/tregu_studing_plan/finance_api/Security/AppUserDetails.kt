package tregu_studing_plan.finance_api.Security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class AppUserDetails(private val user: UserEntity) : UserDetails {

    val id: Long get() = user.id!!

    override fun getAuthorities(): Collection<GrantedAuthority> =
        user.roles.map { SimpleGrantedAuthority(it.toAuthority()) }

    override fun getUsername(): String = user.email!!
    override fun getPassword(): String = user.passwordHash!!
    override fun isAccountNonExpired() = true
    override fun isAccountNonLocked() = true
    override fun isCredentialsNonExpired() = true
    override fun isEnabled() = true
}
