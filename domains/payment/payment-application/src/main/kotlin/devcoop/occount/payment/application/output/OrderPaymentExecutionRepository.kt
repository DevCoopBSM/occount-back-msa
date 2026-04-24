package devcoop.occount.payment.application.output

interface OrderPaymentExecutionRepository {
    fun startProcessing(orderId: Long): OrderPaymentExecutionStartResult

    fun requestCancellation(orderId: Long): OrderPaymentCancellationRequestResult

    fun isCancellationRequested(orderId: Long): Boolean

    fun markCompleted(orderId: Long)

    fun markFailed(orderId: Long)

    fun markCancelled(orderId: Long)
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
