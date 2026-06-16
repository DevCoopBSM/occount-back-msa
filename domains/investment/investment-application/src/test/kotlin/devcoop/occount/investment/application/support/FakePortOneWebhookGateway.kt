package devcoop.occount.investment.application.support

import devcoop.occount.investment.application.output.PortOneWebhookGateway
import devcoop.occount.investment.application.output.WebhookNotification

/**
 * 검증을 통과시키고 미리 지정한 알림을 돌려주는 가짜 게이트웨이.
 * notification 이 null 이면 "관심 없는 이벤트"를 흉내 낸다.
 */
class FakePortOneWebhookGateway(
    private val notification: WebhookNotification?,
) : PortOneWebhookGateway {
    override fun parseAndVerify(rawBody: String, headers: Map<String, String>): WebhookNotification? = notification
}
