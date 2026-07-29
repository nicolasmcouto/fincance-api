package tregu_studing_plan.finance_api.Exception

class SameAccountTransferException :
    RuntimeException("Sender and receiver accounts must be different")
