package tregu_studing_plan.finance_api.Exception

class InsufficientBalanceException(accountId: Long?) :
    RuntimeException("Account $accountId does not have sufficient balance for this transaction")
