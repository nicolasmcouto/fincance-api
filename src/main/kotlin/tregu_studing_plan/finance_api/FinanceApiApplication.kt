package tregu_studing_plan.finance_api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@EnableCaching
@SpringBootApplication
class FinanceApiApplication

fun main(args: Array<String>) {
	runApplication<FinanceApiApplication>(*args)
}
