package devcoop.occount.investment.application.output

/**
 * PortOne 웹훅 원본을 검증·파싱하는 포트.
 * 서명 검증 실패 시 예외를 던지고, 결제 알림이 아닌 이벤트는 null 을 반환한다.
 */
interface PortOneWebhookGateway {
    fun parseAndVerify(rawBody: String, headers: Map<String, String>): WebhookNotification?
}

/**
 * 검증된 PortOne 웹훅 알림(필요한 필드만).
 */
data class WebhookNotification(
    val eventType: String,
    val paymentId: String,
)
