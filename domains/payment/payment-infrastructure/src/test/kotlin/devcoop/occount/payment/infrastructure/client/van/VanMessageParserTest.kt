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
        val response = buildResponse(
            header = "01397010",
            "01",
            "55554444****111*",
            "1000",
            "90",
            "0",
            "00",
            "12345678",
            "20260420",
            "201936",
            "876543210123",
            "0011223344",
            "99887766554433",
            "0300테스트카드체크",
            "0007테스트카드",
            "1",
            "00",
            "정상승인\u001e12345678",
            "IC신용승인",
            "테스트포인트잔여:0\u001e",
        )

        val result = parser.parsePaymentResponse(response)

        assertNotNull(result)
        assertTrue(result.success)
        assertEquals("정상승인 완료", result.message)
        assertEquals("7010", result.transaction?.messageNumber)
        assertEquals(0, result.transaction?.installmentMonths)
        assertEquals("12345678", result.transaction?.approvalNumber)
        assertEquals("876543210123", result.transaction?.transactionId)
        assertEquals("12345678", result.transaction?.terminalId)
        assertEquals("99887766554433", result.transaction?.merchantNumber)
        assertEquals("테스트카드체크", result.card?.acquirerName)
        assertEquals("0300", result.card?.acquirerCode)
        assertEquals("테스트카드", result.card?.issuerName)
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
        val response = buildResponse(
            header = "01397010",
            "01",
            "55554444****111*",
            "1500",
            "90",
            "0",
            "9Q01",
            "TERM-1",
            "20260417",
            "101010",
            "TX-1",
            "ACQ-CODE",
            "MERCHANT-1",
            "0300테스트카드체크",
            "0007테스트카드",
            "2",
            "",
            "IC-1\u001e한도 초과",
            "UUID-1",
        )

        val result = parser.parsePaymentResponse(response)

        assertNotNull(result)
        assertFalse(result.success)
        assertEquals("TRANSACTION_REJECTED", result.errorCode)
        assertEquals("한도 초과", result.transaction?.rejectMessage)
    }

    private fun buildResponse(header: String, vararg fields: String): ByteArray {
        return buildString {
            append('\u0002')
            append(header)
            fields.forEach {
                append('\u001c')
                append(it)
            }
            append('\u0003')
            append('.')
        }.toByteArray(eucKr)
    }

    private fun env(name: String): String = System.getenv(name).orEmpty()
}
