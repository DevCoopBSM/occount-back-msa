package devcoop.occount.investment.infrastructure.webhook

import devcoop.occount.investment.application.exception.WebhookVerificationException
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * PortOne(스탠다드 웹훅) 서명 검증기. 레거시 WebhookVerifier 의 HMAC-SHA256 검증을 이식했다.
 *
 * 서명 대상 문자열은 `"{webhook-id}.{webhook-timestamp}.{payload}"` 이며,
 * 시크릿은 `whsec_` 접두사를 제거한 뒤 Base64 디코딩한 키로 HMAC-SHA256 한다.
 * 타임스탬프는 ±5분 이내만 허용하고, 서명 비교는 타이밍 세이프 비교를 사용한다.
 */
@Component
class PortOneWebhookVerifier {
    /**
     * @param secret PortOne 웹훅 시크릿(whsec_...).
     * @param payload 원본 요청 본문.
     * @param headers 소문자 정규화된 헤더 맵(webhook-id/webhook-signature/webhook-timestamp 포함).
     * @throws WebhookVerificationException 검증 실패 시.
     */
    fun verify(secret: String, payload: String, headers: Map<String, String>) {
        val msgId = headers["webhook-id"]
        val msgSignature = headers["webhook-signature"]
        val msgTimestamp = headers["webhook-timestamp"]
        if (msgId == null || msgSignature == null || msgTimestamp == null) {
            throw WebhookVerificationException()
        }

        verifyTimestamp(msgTimestamp)

        val normalizedSecret = removePrefix(secret)
        val expectedSignature = sign(normalizedSecret, msgId, msgTimestamp, payload)
        val expectedBytes = Base64.getDecoder().decode(expectedSignature)

        val matched = msgSignature.split(" ").any { part ->
            val pair = part.split(",", limit = 2)
            if (pair.size < 2 || pair[0] != SIGNATURE_VERSION) {
                false
            } else {
                runCatching { Base64.getDecoder().decode(pair[1]) }
                    .map { timingSafeEqual(it, expectedBytes) }
                    .getOrDefault(false)
            }
        }

        if (!matched) {
            throw WebhookVerificationException()
        }
    }

    private fun verifyTimestamp(timestampHeader: String) {
        val timestamp = timestampHeader.toLongOrNull() ?: throw WebhookVerificationException()
        val now = Instant.now().epochSecond
        if (Math.abs(now - timestamp) > TOLERANCE_IN_SECONDS) {
            throw WebhookVerificationException()
        }
    }

    private fun sign(secret: String, msgId: String, msgTimestamp: String, payload: String): String {
        val toSign = "$msgId.$msgTimestamp.$payload"
        return try {
            val keySpec = SecretKeySpec(Base64.getDecoder().decode(secret), HMAC_SHA256)
            val mac = Mac.getInstance(HMAC_SHA256)
            mac.init(keySpec)
            val rawHmac = mac.doFinal(toSign.toByteArray(StandardCharsets.UTF_8))
            Base64.getEncoder().encodeToString(rawHmac)
        } catch (e: Exception) {
            throw WebhookVerificationException()
        }
    }

    private fun timingSafeEqual(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) {
            diff = diff or (a[i].toInt() xor b[i].toInt())
        }
        return diff == 0
    }

    private fun removePrefix(secret: String): String =
        if (secret.startsWith(SECRET_PREFIX)) secret.substring(SECRET_PREFIX.length) else secret

    companion object {
        private const val HMAC_SHA256 = "HmacSHA256"
        private const val TOLERANCE_IN_SECONDS = 5 * 60
        private const val SECRET_PREFIX = "whsec_"
        private const val SIGNATURE_VERSION = "v1"
    }
}
