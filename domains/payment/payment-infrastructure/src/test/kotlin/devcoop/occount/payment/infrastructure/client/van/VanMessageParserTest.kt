package devcoop.occount.payment.infrastructure.client.van

import java.nio.charset.Charset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VanMessageParserTest {
    private val eucKr: Charset = Charset.forName("EUC-KR")
    private val protocolSpec = VanProtocolSpec(
        VanProperties(
            host = "localhost",
            port = 5555,
            protocol = VanProperties.Protocol(
                stx = "02",
                etx = "03",
                separator = "1c",
                recordSeparator = "1e",
                blank = "20",
                ack = "06",
                dle = "10",
                formFeed = "0c",
                nak = "15",
                transactionTimeoutSeconds = 30,
            ),
            message = VanProperties.Message(
                paymentServiceType = "0101",
                refundServiceType = "2101",
                transactionType = "01",
                installmentMonths = "00",
            ),
        ),
    )
    private val parser = VanMessageParser(protocolSpec)

    @Test
    fun `승인 응답을 VanResult로 파싱한다`() {
        val response = (
            "\u000201011234" +
                "\u001cTYPE" +
                "\u001c1234********5678" +
                "\u001c1500" +
                "\u001c00" +
                "\u001cN" +
                "\u001cTERM-1" +
                "\u001cMERCHANT-1" +
                "\u001c20260417" +
                "\u001c101010" +
                "\u001cTX-1" +
                "\u001cACQ" +
                "\u001cAcquirer" +
                "\u001cKB VISA" +
                "\u001cISS" +
                "\u001c2" +
                "\u001c정상승인\u001eAPPROVAL-1" +
                "\u001cIC-1" +
                "\u001cUUID-1"
            ).toByteArray(eucKr)

        val result = parser.parsePaymentResponse(response)

        assertNotNull(result)
        assertTrue(result.success)
        assertEquals("정상승인 완료", result.message)
        assertEquals("APPROVAL-1", result.transaction?.approvalNumber)
        assertEquals("TX-1", result.transaction?.transactionId)
        assertEquals("KB", result.card?.cardName)
        assertEquals("VISA", result.card?.cardBrand)
        assertEquals("APPROVED", result.additional?.approvalStatus)
    }

    @Test
    fun `중간 안내 메시지는 null로 처리한다`() {
        val response = (
            "\u000201011234" +
                "\u001cTYPE" +
                "\u001c카드 삽입 후 승인 대기" +
                "\u001c" +
                "\u001c"
            ).toByteArray(eucKr)

        val result = parser.parsePaymentResponse(response)

        assertNull(result)
    }

    @Test
    fun `거절 응답을 실패 결과로 파싱한다`() {
        val response = (
            "\u000201011234" +
                "\u001cTYPE" +
                "\u001c1234********5678" +
                "\u001c1500" +
                "\u001c00" +
                "\u001cN" +
                "\u001cTERM-1" +
                "\u001cMERCHANT-1" +
                "\u001c20260417" +
                "\u001c101010" +
                "\u001cTX-1" +
                "\u001cACQ" +
                "\u001cAcquirer" +
                "\u001cKB VISA" +
                "\u001cISS" +
                "\u001c2" +
                "\u001c9Q" +
                "\u001cIC-1\u001e한도 초과" +
                "\u001cUUID-1"
            ).toByteArray(eucKr)

        val result = parser.parsePaymentResponse(response)

        assertNotNull(result)
        assertFalse(result.success)
        assertEquals("TRANSACTION_REJECTED", result.errorCode)
        assertEquals("한도 초과", result.transaction?.rejectMessage)
    }
}
