package devcoop.occount.order.application.config

data class OrderTimeoutConfig(
    val timeoutSeconds: Long,
    val asyncTimeoutBufferMillis: Long,
)
