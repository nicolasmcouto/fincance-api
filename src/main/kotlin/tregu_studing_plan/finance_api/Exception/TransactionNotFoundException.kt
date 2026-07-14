package tregu_studing_plan.finance_api.Exception

class TransactionNotFoundException(id: Long) : RuntimeException("Transaction $id not found")
