package devcoop.occount.order.application.support

internal object OrderRetryPolicy {
    const val MAX_RETRY_COUNT = 3
    const val BASE_BACKOFF_MILLIS = 50L
}
