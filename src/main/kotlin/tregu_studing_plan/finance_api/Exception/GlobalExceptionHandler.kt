package tregu_studing_plan.finance_api.Exception

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.time.LocalDateTime

data class FieldErrorResponse(
    val field: String,
    val message: String
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiError(
    val timestamp: LocalDateTime,
    val status: Int,
    val error: String,
    val message: String,
    val fields: List<FieldErrorResponse>? = null
)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException::class)
    fun handleAccountNotFound(ex: AccountNotFoundException) = build(HttpStatus.NOT_FOUND, ex.message)

    @ExceptionHandler(TransactionNotFoundException::class)
    fun handleTransactionNotFound(ex: TransactionNotFoundException) = build(HttpStatus.NOT_FOUND, ex.message)

    @ExceptionHandler(InsufficientBalanceException::class)
    fun handleInsufficientBalance(ex: InsufficientBalanceException) = build(HttpStatus.UNPROCESSABLE_ENTITY, ex.message)

    @ExceptionHandler(SameAccountTransferException::class)
    fun handleSameAccountTransfer(ex: SameAccountTransferException) = build(HttpStatus.UNPROCESSABLE_ENTITY, ex.message)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val fields = ex.bindingResult.fieldErrors.map {error ->
            FieldErrorResponse(
                field = error.field,
                message = error.defaultMessage ?: "Valor invalido"
        )
        }
        return build(HttpStatus.BAD_REQUEST, "Dados invalidos", fields)
    }
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnReadableException() = build(HttpStatus.BAD_REQUEST,"Json invalido ou campo obrigatorio ausente")

    private fun build(
        status: HttpStatus,
        message: String?,
        field: List<FieldErrorResponse>? = null
    ): ResponseEntity<ApiError> {

        val body = ApiError(
            timestamp = LocalDateTime.now(),
            status = status.value(),
            error = status.reasonPhrase,
            message = message ?: status.reasonPhrase,
            fields = field,
        )
        return ResponseEntity.status(status).body(body)
    }
}
