package tregu_studing_plan.finance_api.Security

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table

enum class Role {
    USER, MANAGER, ADMIN;

    // O prefixo ROLE_ vive aqui, num lugar só. Nunca espalhe essa string.
    fun toAuthority(): String = "ROLE_$name"
}

@Table(name = "users")
@Entity
class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(unique = true, nullable = false)
    var email: String? = null

    @Column(nullable = false)
    var passwordHash: String? = null

    @ElementCollection(fetch = FetchType.EAGER)   // EAGER: as roles são necessárias em toda request
    @CollectionTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id")]
    )
    @Enumerated(EnumType.STRING)                  // salva "ADMIN", não 2 — nunca use ORDINAL
    @Column(name = "role", nullable = false)
    val roles: MutableSet<Role> = mutableSetOf(Role.USER)

}