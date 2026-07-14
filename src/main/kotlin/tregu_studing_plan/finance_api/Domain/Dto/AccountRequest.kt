package tregu_studing_plan.finance_api.Domain.Dto

import tregu_studing_plan.finance_api.Domain.Enum.AccountType
import java.math.BigDecimal

data class AccountRequest(
    val ownerName: String,
    val type: AccountType,
    val balance: BigDecimal,
)
