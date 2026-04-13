package devcoop.occount.payment.infrastructure.client.pg.protocol

import java.nio.charset.Charset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PgMessageParserTest {
    @Test
    fun `parse payment response returns success result for approval message`() {
        val response = buildResponse(
            "0103MSG0001",
            "D1",
            "1234********1234",
            "1500",
            "00",
            "",
            "TERM01",
            "MERCHANT01",
            "20260403",
            "101010",
            "TX-1",
            "ACQ01",
            "Acquirer",
            "Issuer VISA",
            "ISS01",
            "2",
            "정상승인\u001eAPPROVAL1",
            "IC",
            "UUID-1",
        )

        val result = PgMessageParser.parsePaymentResponse(response)

        assertNotNull(result)
        assertEquals(true, result.success)
        assertEquals("APPROVAL1", result.transaction?.approvalNumber)
        assertEquals("VISA", result.card?.cardBrand)
        assertEquals("Issuer", result.card?.cardName)
    }

    @Test
    fun `parse payment response returns null when approval number is missing`() {
        val response = buildResponse(
            "0103MSG0001",
            "D1",
            "카드 삽입",
            "",
            "",
        )

        val result = PgMessageParser.parsePaymentResponse(response)

        assertNull(result)
    }

    private fun buildResponse(vararg fields: String): ByteArray {
        return ("\u0002" + fields.joinToString("\u001c") + "\u0003").toByteArray(Charset.forName("EUC-KR"))
    }
}
