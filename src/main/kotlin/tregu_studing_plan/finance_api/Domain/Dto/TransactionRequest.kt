package tregu_studing_plan.finance_api.Domain.Dto

import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import tregu_studing_plan.finance_api.Domain.Enum.TransactionType
import java.math.BigDecimal
import java.time.LocalDateTime

data class TransactionRequest(
    @field:NotBlank(message = "A descricao é obrigatoria")
    @Size(max = 255, message = "A descricao pode ter no maximo 255 caracteres")
    val description: String,
    @Positive(message = "O valor da transferencia precisa ser maior que 0")
    @field:Digits(integer = 17, fraction = 2, message = "O valor deve ter no máximo 2 casas decimais")
    val amount: BigDecimal,
    val type: TransactionType,
    @PastOrPresent(message = "A transferencia nao pode estar no fututo")
    val date: LocalDateTime,
    @Positive(message = "O Id precisa ser positivo")
    val senderId: Long,
    @Positive(message = "O Id precisa ser positivo")
    val receiverId: Long,
)
