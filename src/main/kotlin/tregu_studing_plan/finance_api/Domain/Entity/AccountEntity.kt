package tregu_studing_plan.finance_api.Domain.Entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import tregu_studing_plan.finance_api.Domain.Enum.AccountType
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "accounts")
class AccountEntity(
    @Column(nullable = false)
    var ownerName: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: AccountType,

    @Column(nullable = false)
    var balance: BigDecimal,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
)
