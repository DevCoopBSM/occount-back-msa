package devcoop.occount.investment.infrastructure.webhook

import devcoop.occount.investment.application.output.PortOneWebhookGateway
import devcoop.occount.investment.application.output.WebhookNotification
import devcoop.occount.investment.infrastructure.client.PortOneProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/**
 * PortOne 웹훅 원본을 검증·파싱하는 어댑터.
 * 서명 검증 후 본문을 키 그대로 Map 으로 파싱해(전역 snake_case 전략 영향 없이) type/data.paymentId 만 취한다.
 */
@Component
@EnableConfigurationProperties(PortOneProperties::class)
class PortOneWebhookGatewayImpl(
    private val verifier: PortOneWebhookVerifier,
    private val properties: PortOneProperties,
    private val objectMapper: ObjectMapper,
) : PortOneWebhookGateway {

    override fun parseAndVerify(rawBody: String, headers: Map<String, String>): WebhookNotification? {
        val normalizedHeaders = headers.entries.associate { it.key.lowercase() to it.value }
        verifier.verify(properties.webhookSecret, rawBody, normalizedHeaders)

        val root = objectMapper.readValue(rawBody, Map::class.java)
        val eventType = root["type"] as? String ?: return null
        val paymentId = (root["data"] as? Map<*, *>)?.get("paymentId") as? String ?: return null
        return WebhookNotification(eventType = eventType, paymentId = paymentId)
    }
}
