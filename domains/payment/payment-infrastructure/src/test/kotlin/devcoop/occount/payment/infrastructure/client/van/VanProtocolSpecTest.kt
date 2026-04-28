package devcoop.occount.payment.infrastructure.client.van

import kotlin.test.Test
import kotlin.test.assertEquals

class VanProtocolSpecTest {
    private val message = VanProperties.Message(
        paymentServiceType = "0101",
        refundServiceType = "2101",
        terminalCloseServiceType = "9999",
        terminalCloseFiller = "CLOSE",
        transactionType = "D1",
        installmentMonths = "00",
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
            message = message,
        ),
    )

    @Test
    fun `splitFrames separates concatenated STX messages and control signals`() {
        val bytes = byteArrayOf(
            0x02, 0x30, 0x30, 0x33, 0x03, 0x31,
            0x02, 0x30, 0x30, 0x34, 0x03, 0x32,
            0x06, 0x06, 0x06,
            0x0c, 0x0c, 0x0c,
            0x10,
        )

        val frames = protocolSpec.splitFrames(bytes)

        assertEquals(5, frames.size)
        assertEquals("023030330331", protocolSpec.toHex(frames[0]))
        assertEquals("023030340332", protocolSpec.toHex(frames[1]))
        assertEquals("060606", protocolSpec.toHex(frames[2]))
        assertEquals("0c0c0c", protocolSpec.toHex(frames[3]))
        assertEquals("10", protocolSpec.toHex(frames[4]))
    }
}
