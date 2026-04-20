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
    private val protocolCodes = VanProtocolCodes(
        cancelMessageType = env("VAN_PROTOCOL_CODES_CANCEL_MESSAGE_TYPE"),
        rejectStatusPrefix = env("VAN_PROTOCOL_CODES_REJECT_STATUS_PREFIX"),
        cardInsertKeyword = env("VAN_PROTOCOL_CODES_CARD_INSERT_KEYWORD"),
    )
    private val protocolSpec = VanProtocolSpec(
        VanProperties(
            terminals = mapOf(1 to VanProperties.Terminal(host = "localhost", port = 5555)),
            protocol = VanProperties.Protocol(
                stx = env("VAN_API_PROTOCOL_STX"),
                etx = env("VAN_API_PROTOCOL_ETX"),
                separator = env("VAN_API_PROTOCOL_SEPARATOR"),
                recordSeparator = env("VAN_API_PROTOCOL_RECORD_SEPARATOR"),
                blank = env("VAN_API_PROTOCOL_BLANK"),
                ack = env("VAN_API_PROTOCOL_ACK"),
                dle = env("VAN_API_PROTOCOL_DLE"),
                formFeed = env("VAN_API_PROTOCOL_FORM_FEED"),
                nak = env("VAN_API_PROTOCOL_NAK"),
                transactionTimeoutSeconds = env("VAN_API_PROTOCOL_TRANSACTION_TIMEOUT_SECONDS").toLongOrNull() ?: 30L,
            ),
            message = VanProperties.Message(
                paymentServiceType = env("VAN_API_MESSAGE_PAYMENT_SERVICE_TYPE"),
                refundServiceType = env("VAN_API_MESSAGE_REFUND_SERVICE_TYPE"),
                terminalCloseServiceType = env("VAN_API_MESSAGE_TERMINAL_CLOSE_SERVICE_TYPE"),
                terminalCloseFiller = env("VAN_API_MESSAGE_TERMINAL_CLOSE_FILLER"),
                transactionType = env("VAN_API_MESSAGE_TRANSACTION_TYPE"),
                installmentMonths = env("VAN_API_MESSAGE_INSTALLMENT_MONTHS"),
            ),
        ),
    )
    private val responseParser = VanResponseParser(protocolSpec, protocolCodes)
    private val parser = VanMessageParser(protocolSpec, responseParser)

    @Test
    fun `승인 응답을 VanResult로 파싱한다`() {
        val response = "0230313937303130321c30311c34363139353431302a2a2a2a3232322a1c313030301c39301c301c30301c32373036383132321c32303236303432301c3230343634311c3731303630343938363331361c303132363732313634361c32393736323332303034303037391c30333030bdc5c7d1c4abb5e5c3bcc5a91c30303037bdc5c7d1c4abb5e51c311c30301cc1a4bbf3bdc2c0ce1e32373036383132321c4943bdc5bfebbdc2c0ce1cb8b6c0ccbdc5c7d150c0dcbfa93a3637321e1c1c1c1c1c1c032e"
            .hexToByteArray()

        val result = parser.parsePaymentResponse(response)

        assertNotNull(result)
        assertTrue(result.success)
        assertEquals("정상승인 완료", result.message)
        assertEquals("0102", result.transaction?.messageNumber)
        assertEquals(0, result.transaction?.installmentMonths)
        assertEquals("27068122", result.transaction?.approvalNumber)
        assertEquals("710604986316", result.transaction?.transactionId)
        assertEquals("27068122", result.transaction?.terminalId)
        assertEquals("29762320040079", result.transaction?.merchantNumber)
        assertEquals("신한카드체크", result.card?.acquirerName)
        assertEquals("0300", result.card?.acquirerCode)
        assertEquals("신한카드", result.card?.issuerName)
        assertEquals("0007", result.card?.issuerCode)
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
            "\u000201970102" +
                "\u001c01" +
                "\u001c1234********5678" +
                "\u001c1500" +
                "\u001c90" +
                "\u001c0" +
                "\u001c9Q01" +
                "\u001cTERM-1" +
                "\u001c20260417" +
                "\u001c101010" +
                "\u001cTX-1" +
                "\u001cACQ-CODE" +
                "\u001cMERCHANT-1" +
                "\u001c0300Acquirer" +
                "\u001c0007KB VISA" +
                "\u001c2" +
                "\u001c" +
                "\u001cIC-1\u001e한도 초과" +
                "\u001cUUID-1"
            ).toByteArray(eucKr)

        val result = parser.parsePaymentResponse(response)

        assertNotNull(result)
        assertFalse(result.success)
        assertEquals("TRANSACTION_REJECTED", result.errorCode)
        assertEquals("한도 초과", result.transaction?.rejectMessage)
    }

    private fun String.hexToByteArray(): ByteArray {
        return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    private fun env(name: String): String = System.getenv(name).orEmpty()
}
