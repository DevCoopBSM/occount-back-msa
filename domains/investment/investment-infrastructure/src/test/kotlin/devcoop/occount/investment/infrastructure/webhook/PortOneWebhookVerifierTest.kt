package devcoop.occount.investment.infrastructure.webhook

import devcoop.occount.investment.application.exception.WebhookVerificationException
import org.junit.jupiter.api.assertThrows
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.Test

class PortOneWebhookVerifierTest {
    private val verifier = PortOneWebhookVerifier()

    // 임의의 Base64 인코딩 키(테스트 전용, 실제 시크릿 아님).
    private val rawKey = Base64.getEncoder().encodeToString("test-webhook-key".toByteArray())
    private val secret = "whsec_$rawKey"

    private fun sign(msgId: String, timestamp: String, payload: String): String {
        val toSign = "$msgId.$timestamp.$payload"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(Base64.getDecoder().decode(rawKey), "HmacSHA256"))
        val raw = mac.doFinal(toSign.toByteArray(StandardCharsets.UTF_8))
        return Base64.getEncoder().encodeToString(raw)
    }

    private fun headers(msgId: String, timestamp: String, signature: String): Map<String, String> =
        mapOf(
            "webhook-id" to msgId,
            "webhook-timestamp" to timestamp,
            "webhook-signature" to "v1,$signature",
        )

    @Test
    fun `verify passes for a valid signature`() {
        val body = """{"type":"Transaction.Paid","data":{"paymentId":"I-abc"}}"""
        val msgId = "msg_1"
        val ts = Instant.now().epochSecond.toString()

        verifier.verify(secret, body, headers(msgId, ts, sign(msgId, ts, body)))
    }

    @Test
    fun `verify rejects a tampered payload`() {
        val msgId = "msg_1"
        val ts = Instant.now().epochSecond.toString()
        val signature = sign(msgId, ts, """{"type":"Transaction.Paid","data":{"paymentId":"I-abc"}}""")

        assertThrows<WebhookVerificationException> {
            verifier.verify(secret, """{"type":"Transaction.Paid","data":{"paymentId":"I-EVIL"}}""", headers(msgId, ts, signature))
        }
    }

    @Test
    fun `verify rejects a stale timestamp`() {
        val body = """{"type":"Transaction.Paid"}"""
        val msgId = "msg_1"
        val staleTs = (Instant.now().epochSecond - 3600).toString()

        assertThrows<WebhookVerificationException> {
            verifier.verify(secret, body, headers(msgId, staleTs, sign(msgId, staleTs, body)))
        }
    }

    @Test
    fun `verify rejects missing headers`() {
        assertThrows<WebhookVerificationException> {
            verifier.verify(secret, "{}", emptyMap())
        }
    }
}
