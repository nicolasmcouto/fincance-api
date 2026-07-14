package tregu_studing_plan.finance_api.Exception

class AccountNotFoundException(id: Long) : RuntimeException("Account $id not found")
