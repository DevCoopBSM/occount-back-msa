package devcoop.occount.payment.infrastructure.client.van

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VanProtocolSpecTest {
    private val protocolSpec = VanTestFixtures.protocolSpec

    @Test
    fun `splitFrames separates concatenated STX messages and control signals`() {
        val bytes = byteArrayOf(
            protocolSpec.stxByte, 0x30, 0x30, 0x33, protocolSpec.etxByte, 0x31,
            protocolSpec.stxByte, 0x30, 0x30, 0x34, protocolSpec.etxByte, 0x32,
            protocolSpec.ackByte, protocolSpec.ackByte, protocolSpec.ackByte,
            protocolSpec.formFeedByte, protocolSpec.formFeedByte, protocolSpec.formFeedByte,
            protocolSpec.dleByte,
        )

        val frames = protocolSpec.splitFrames(bytes)

        assertEquals(5, frames.size)
        assertFrameEquals(
            byteArrayOf(protocolSpec.stxByte, 0x30, 0x30, 0x33, protocolSpec.etxByte, 0x31),
            frames[0],
            "first STX frame should match",
        )
        assertFrameEquals(
            byteArrayOf(protocolSpec.stxByte, 0x30, 0x30, 0x34, protocolSpec.etxByte, 0x32),
            frames[1],
            "second STX frame should match",
        )
        assertFrameEquals(protocolSpec.ackBytes, frames[2], "ACK signal frame should match")
        assertFrameEquals(repeated(protocolSpec.formFeedByte), frames[3], "form feed signal frame should match")
        assertFrameEquals(byteArrayOf(protocolSpec.dleByte), frames[4], "DLE signal frame should match")
    }

    private fun assertFrameEquals(expected: ByteArray, actual: ByteArray, message: String) {
        assertTrue(actual.contentEquals(expected), message)
    }

    private fun repeated(value: Byte): ByteArray {
        return ByteArray(SIGNAL_BYTE_COUNT) { value }
    }

    companion object {
        private const val SIGNAL_BYTE_COUNT = 3
    }
}
