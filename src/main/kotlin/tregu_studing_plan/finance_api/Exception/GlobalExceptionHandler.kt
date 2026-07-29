package tregu_studing_plan.finance_api.Exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

data class ApiError(
    val timestamp: LocalDateTime,
    val status: Int,
    val error: String,
    val message: String,
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

    private fun build(status: HttpStatus, message: String?): ResponseEntity<ApiError> {
        val body = ApiError(
            timestamp = LocalDateTime.now(),
            status = status.value(),
            error = status.reasonPhrase,
            message = message ?: status.reasonPhrase,
        )
        return ResponseEntity.status(status).body(body)
    }
}
