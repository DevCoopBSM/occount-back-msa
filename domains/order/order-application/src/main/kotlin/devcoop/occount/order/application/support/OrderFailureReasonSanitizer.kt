package devcoop.occount.order.application.support

object OrderFailureReasonSanitizer {
    private const val MAX_FAILURE_REASON_LENGTH = 255

    fun sanitize(reason: String?): String? {
        return reason?.take(MAX_FAILURE_REASON_LENGTH)
    }
}
