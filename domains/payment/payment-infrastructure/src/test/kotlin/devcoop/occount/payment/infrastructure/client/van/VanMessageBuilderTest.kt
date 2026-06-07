package devcoop.occount.payment.infrastructure.client.van

import devcoop.occount.payment.application.dto.request.ItemCommand
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VanMessageBuilderTest {
    private val protocolSpec = VanTestFixtures.protocolSpec
    private val properties = VanTestFixtures.properties
    private val builder = VanMessageBuilder(protocolSpec, properties)

    private val stx = protocolSpec.stxByte
    private val etx = protocolSpec.etxByte
    private val sep = protocolSpec.separatorByte
    private val blank = protocolSpec.blankByte

    @Test
    fun `buildRefundMessage with empty items keeps pre-change wire format`() {
        val bytes = builder.buildRefundMessage(
            amount = 1500,
            approvalDate = "20260512",
            approvalNumber = "12345678",
            items = emptyList(),
        )

        val expectedBody = encodeBody(
            properties.message.refundServiceType,
            properties.message.transactionType,
            "1500",
            properties.message.installmentMonths,
            "12345678",
            "20260512",
            productData = ByteArray(0),
        )
        val expected = wrap(expectedBody)
        assertContentEquals(expected, bytes, "items=empty 환불 와이어가 변경되면 안 됨 (회귀 가드)")
    }

    @Test
    fun `buildRefundMessage with items embeds product data between approvalDate and blank`() {
        val items = listOf(
            ItemCommand(name = "커피", price = 3000, quantity = 2, total = 6000),
            ItemCommand(name = "쿠키", price = 1500, quantity = 1, total = 1500),
        )
        val productData = VanReceiptBuilder.buildReceiptLines(items)
        assertTrue(productData.isNotEmpty(), "fixture sanity: 상품 라인이 비어있지 않아야 함")

        val bytes = builder.buildRefundMessage(
            amount = 7500,
            approvalDate = "20260512",
            approvalNumber = "87654321",
            items = items,
        )

        val expectedBody = encodeBody(
            properties.message.refundServiceType,
            properties.message.transactionType,
            "7500",
            properties.message.installmentMonths,
            "87654321",
            "20260512",
            productData = productData,
        )
        val expected = wrap(expectedBody)
        assertContentEquals(expected, bytes)

        val productSegment = byteArrayOf(sep, sep) + productData + byteArrayOf(sep, blank)
        assertTrue(
            bytes.indexOfSubsequence(productSegment) >= 0,
            "approvalDate sep sep [productData] sep blank 패턴이 포함돼야 함",
        )
    }

    @Test
    fun `length header reflects body size including product data`() {
        val items = listOf(ItemCommand(name = "라떼", price = 4500, quantity = 1, total = 4500))
        val bytes = builder.buildRefundMessage(
            amount = 4500,
            approvalDate = "20260512",
            approvalNumber = "11111111",
            items = items,
        )

        val lengthAscii = String(bytes.copyOfRange(1, 5), Charsets.US_ASCII)
        val declared = lengthAscii.toInt()
        assertEquals(bytes.size, declared, "length 헤더는 STX~BCC 총 바이트 수와 일치해야 함")
    }

    @Test
    fun `product line encodes Korean name as EUC-KR (not UTF-8)`() {
        val item = ItemCommand(name = "커피", price = 3000, quantity = 1, total = 3000)
        val bytes = builder.buildRefundMessage(
            amount = 3000,
            approvalDate = "20260512",
            approvalNumber = "22222222",
            items = listOf(item),
        )

        val coffeeEucKr = byteArrayOf(0xc4.toByte(), 0xbf.toByte(), 0xc7.toByte(), 0xc7.toByte())
        assertTrue(
            bytes.indexOfSubsequence(coffeeEucKr) >= 0,
            "상품명 '커피'가 EUC-KR(C4 BF C7 C7)로 인코딩돼야 함 — UTF-8(EC BB BF ED 94 BC)이면 즉시 실패",
        )

        val gaeEucKr = byteArrayOf(0xb0.toByte(), 0xb3.toByte())
        assertTrue(
            bytes.indexOfSubsequence(gaeEucKr) >= 0,
            "수량 단위 '개'가 EUC-KR(B0 B3)로 인코딩돼야 함",
        )

        val wonEucKr = byteArrayOf(0xbf.toByte(), 0xf8.toByte())
        assertTrue(
            bytes.indexOfSubsequence(wonEucKr) >= 0,
            "금액 단위 '원'이 EUC-KR(BF F8)로 인코딩돼야 함",
        )

        val coffeeUtf8 = "커피".toByteArray(Charsets.UTF_8)
        assertEquals(
            -1,
            bytes.indexOfSubsequence(coffeeUtf8),
            "UTF-8 인코딩 바이트가 나타나면 안 됨",
        )
    }

    @Test
    fun `BCC is XOR of bytes after STX through ETX OR-ed with blank`() {
        val bytes = builder.buildRefundMessage(
            amount = 1000,
            approvalDate = "20260512",
            approvalNumber = "00000001",
            items = emptyList(),
        )

        var xor = 0
        for (i in 1 until bytes.size - 1) {
            xor = xor xor (bytes[i].toInt() and 0xff)
            if (bytes[i] == etx) break
        }
        val expectedBcc = (xor or (blank.toInt() and 0xff)).toByte()
        assertEquals(expectedBcc, bytes.last(), "BCC = (STX 다음 ~ ETX XOR) OR blank")
    }

    private fun encodeBody(
        serviceType: String,
        transactionType: String,
        amountString: String,
        installmentMonths: String,
        approvalNumber: String,
        approvalDate: String,
        productData: ByteArray,
    ): ByteArray {
        val out = mutableListOf<Byte>()
        out += serviceType.toByteArray(Charsets.US_ASCII).toList()
        out += sep
        out += transactionType.toByteArray(Charsets.US_ASCII).toList()
        out += sep
        out += amountString.toByteArray(Charsets.US_ASCII).toList()
        out += sep
        out += sep
        out += sep
        out += installmentMonths.toByteArray(Charsets.US_ASCII).toList()
        out += sep
        out += approvalNumber.toByteArray(Charsets.US_ASCII).toList()
        out += sep
        out += approvalDate.toByteArray(Charsets.US_ASCII).toList()
        out += sep
        out += sep
        out += productData.toList()
        out += sep
        out += blank
        out += sep
        out += sep
        out += sep
        out += etx
        return out.toByteArray()
    }

    private fun wrap(body: ByteArray): ByteArray {
        val totalLength = 1 + 4 + body.size + 1
        val lengthAscii = totalLength.toString().padStart(4, '0').toByteArray(Charsets.US_ASCII)
        val withoutBcc = byteArrayOf(stx) + lengthAscii + body
        var xor = 0
        for (i in 1 until withoutBcc.size) {
            xor = xor xor (withoutBcc[i].toInt() and 0xff)
            if (withoutBcc[i] == etx) break
        }
        val bcc = (xor or (blank.toInt() and 0xff)).toByte()
        return withoutBcc + bcc
    }

    private fun assertContentEquals(expected: ByteArray, actual: ByteArray, message: String? = null) {
        assertEquals(expected.size, actual.size, message ?: "byte length mismatch")
        for (i in expected.indices) {
            assertEquals(
                expected[i],
                actual[i],
                "byte[$i] expected=0x%02x actual=0x%02x %s".format(
                    expected[i].toInt() and 0xff,
                    actual[i].toInt() and 0xff,
                    message ?: "",
                ),
            )
        }
    }

    private fun ByteArray.indexOfSubsequence(sub: ByteArray): Int {
        if (sub.isEmpty()) return 0
        outer@ for (i in 0..this.size - sub.size) {
            for (j in sub.indices) {
                if (this[i + j] != sub[j]) continue@outer
            }
            return i
        }
        return -1
    }
}
