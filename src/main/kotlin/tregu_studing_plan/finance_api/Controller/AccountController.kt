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
import tregu_studing_plan.finance_api.Domain.Dto.AccountRequest
import tregu_studing_plan.finance_api.Domain.Entity.AccountEntity
import tregu_studing_plan.finance_api.Service.AccountService

@RestController
@RequestMapping("/accounts")
class AccountController(
    private val accountService: AccountService,
) {

    @GetMapping
    fun findAll(): List<AccountEntity> = accountService.findAll()

    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long): AccountEntity = accountService.findById(id)

    @PostMapping
    fun create(@RequestBody request: AccountRequest): ResponseEntity<AccountEntity> {
        val created = accountService.create(
            AccountEntity(
                ownerName = request.ownerName,
                type = request.type,
                balance = request.balance,
            ),
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody request: AccountRequest): AccountEntity =
        accountService.update(
            id,
            AccountEntity(
                ownerName = request.ownerName,
                type = request.type,
                balance = request.balance,
            ),
        )

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        accountService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
