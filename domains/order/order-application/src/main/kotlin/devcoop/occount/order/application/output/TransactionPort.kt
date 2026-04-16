package devcoop.occount.order.application.output

interface TransactionPort {
    fun <T : Any> executeInNewTransaction(action: () -> T): T
}
