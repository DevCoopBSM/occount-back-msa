package devcoop.occount.payment.application.output

interface OrderPaymentExecutionRepository {
    fun startProcessing(orderId: String): OrderPaymentExecutionStartResult

    fun requestCancellation(orderId: String): OrderPaymentCancellationRequestResult

    fun isCancellationRequested(orderId: String): Boolean

    fun markCompleted(orderId: String)

    fun markFailed(orderId: String)

    fun markCancelled(orderId: String)
}

enum class OrderPaymentExecutionState {
    CANCELLATION_REQUESTED,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED,
}

enum class OrderPaymentExecutionStartResult {
    STARTED,
    CANCELLED_BEFORE_START,
    DUPLICATE,
}

enum class OrderPaymentCancellationRequestResult {
    TERMINAL_CANCELLATION_REQUIRED,
    NO_ACTIVE_PAYMENT,
}
