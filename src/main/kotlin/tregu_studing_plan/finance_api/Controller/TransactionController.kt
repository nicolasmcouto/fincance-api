package tregu_studing_plan.finance_api.Controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tregu_studing_plan.finance_api.Domain.Dto.TransactionRequest
import tregu_studing_plan.finance_api.Domain.Entity.TransactionEntity
import tregu_studing_plan.finance_api.Service.AccountService
import tregu_studing_plan.finance_api.Service.TransactionService

@RestController
@RequestMapping("/transactions")
class TransactionController(
    private val transactionService: TransactionService,
    private val accountService: AccountService,
) {

    @GetMapping
    fun findAll(): List<TransactionEntity> = transactionService.findAll()

    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long): TransactionEntity = transactionService.findById(id)

    @PostMapping
    fun create(@RequestBody request: TransactionRequest): ResponseEntity<TransactionEntity> {
        val created = transactionService.create(toEntity(request))
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody request: TransactionRequest): TransactionEntity =
        transactionService.update(id, toEntity(request))

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        transactionService.delete(id)
        return ResponseEntity.noContent().build()
    }

    private fun toEntity(request: TransactionRequest) = TransactionEntity(
        description = request.description,
        amount = request.amount,
        type = request.type,
        date = request.date,
        sender = accountService.findById(request.senderId),
        receiver = accountService.findById(request.receiverId),
    )
}
