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
<<<<<<< HEAD
    private val protocolSpec = VanTestFixtures.protocolSpec
    private val parser = VanTestFixtures.messageParser()
    private val recordSeparator = VanTestFixtures.recordSeparatorChar
=======
    private val protocolCodes = VanProtocolCodes(
        cancelMessageType = "2102",
        rejectStatusPrefix = "9",
        cardInsertKeyword = "카드 삽입",
    )
    private val protocolSpec = VanProtocolSpec(
        VanProperties(
            terminals = mapOf(1 to VanProperties.Terminal(host = "localhost", port = 5555)),
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
                transactionTimeoutSeconds = 30L,
            ),
            message = VanProperties.Message(
                paymentServiceType = "0101",
                refundServiceType = "2101",
                terminalCloseServiceType = "9999",
                terminalCloseFiller = "CLOSE",
                transactionType = "D1",
                installmentMonths = "00",
            ),
        ),
    )
    private val responseParser = VanResponseParser(protocolSpec, protocolCodes)
    private val parser = VanMessageParser(protocolSpec, responseParser)
>>>>>>> ae03c36 (test(payment): VAN 취소 전문 테스트 보강)

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
            "정상승인${recordSeparator}12345678",
            "IC신용승인",
            "테스트포인트잔여:0${recordSeparator}",
        )

        val result = parser.parsePaymentResponse(response)

        assertNotNull(result)
        assertTrue(result.success)
        assertEquals("정상승인 완료", result.message)
        assertEquals("7010", result.transaction?.messageNumber)
        assertEquals(0, result.transaction?.installmentMonths)
        assertEquals("12345678", result.transaction?.approvalNumber)
        assertEquals("20260420", result.transaction?.approvalDate)
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
        val response = buildString {
            append(protocolSpec.stxByte.toProtocolChar())
            append("01011234")
            append(protocolSpec.separatorChar)
            append("TYPE")
            append(protocolSpec.separatorChar)
            append("TEST_INSERT WAIT")
            append(protocolSpec.separatorChar)
            append(protocolSpec.separatorChar)
        }.toByteArray(eucKr)

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
            "ZX01",
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
            "IC-1${recordSeparator}한도 초과",
            "UUID-1",
        )

        val result = parser.parsePaymentResponse(response)

        assertNotNull(result)
        assertFalse(result.success)
        assertEquals("TRANSACTION_REJECTED", result.errorCode)
        assertEquals("한도 초과", result.transaction?.rejectMessage)
    }

    @Test
    fun `취소 응답은 승인번호가 없어도 성공으로 파싱한다`() {
        val cancelParser = cancelParser()
        val response = buildResponse(
            header = "00842102",
            "0203",
            "55554444****111*",
            "1000",
            "90",
            "0",
            "00",
            "267733358",
            "20260420",
            "201936",
            "876543210123",
            "0011223344",
            "99887766554433",
            "0300테스트카드체크",
            "0007테스트카드",
            "1",
            "00",
            "IC신용취소",
            "테스트포인트잔여:0\u001e",
        )

        val result = cancelParser.parsePaymentResponse(response)

        assertNotNull(result)
        assertTrue(result.success)
        assertEquals("정상취소 완료", result.message)
        assertEquals("20260420", result.transaction?.approvalDate)
        assertEquals("267733358", result.transaction?.terminalId)
        assertEquals("CANCELLED", result.additional?.approvalStatus)
    }

    private fun buildResponse(header: String, vararg fields: String): ByteArray {
        return buildString {
            append(protocolSpec.stxByte.toProtocolChar())
            append(header)
            fields.forEach {
                append(protocolSpec.separatorChar)
                append(it)
            }
            append(protocolSpec.etxByte.toProtocolChar())
            append('.')
        }.toByteArray(eucKr)
    }
<<<<<<< HEAD

    private fun Byte.toProtocolChar(): Char {
        return (toInt() and 0xff).toChar()
    }
=======
>>>>>>> ae03c36 (test(payment): VAN 취소 전문 테스트 보강)
}
