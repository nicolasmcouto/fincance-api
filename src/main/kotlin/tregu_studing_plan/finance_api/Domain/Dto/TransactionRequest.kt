package tregu_studing_plan.finance_api.Domain.Dto

import tregu_studing_plan.finance_api.Domain.Enum.TransactionType
import java.math.BigDecimal
import java.time.LocalDateTime

data class TransactionRequest(
    val description: String,
    val amount: BigDecimal,
    val type: TransactionType,
    val date: LocalDateTime,
    val accountId: Long,
)
