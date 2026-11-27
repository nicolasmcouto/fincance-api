package tregu_studing_plan.finance_api.Domain.Dto

import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import tregu_studing_plan.finance_api.Domain.Enum.AccountType
import java.math.BigDecimal

data class AccountRequest(
    @field:NotBlank("O nome do dono da conta nao pode estar em branco")
    @Size(max = 40, message = "O tamanho do nome é muito grando, usei abreviacoes")
    val ownerName: String,
    val type: AccountType,
    @PositiveOrZero(message = "O valor do no capital inicial, nao pode ser negativo")
    @Digits(integer = 17, fraction = 2, message = "O valor do no capital inicial deve haver apenas 2 casas decimasis")
    val balance: BigDecimal,
)
