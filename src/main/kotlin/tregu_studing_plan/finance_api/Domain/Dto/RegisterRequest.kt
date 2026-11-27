package tregu_studing_plan.finance_api.Domain.Dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @NotBlank(message = "O email é obrigatorio")
    @Email(message = "O email é invalido")
    val email: String,
    @NotBlank(message ="A senha é obrigatoria")
    @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
    val password: String)