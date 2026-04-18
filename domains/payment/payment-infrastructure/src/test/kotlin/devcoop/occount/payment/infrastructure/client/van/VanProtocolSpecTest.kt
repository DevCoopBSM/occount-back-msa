package devcoop.occount.payment.infrastructure.client.van

import kotlin.test.Test
import kotlin.test.assertEquals

class VanProtocolSpecTest {
    private val message = VanProperties.Message(
        paymentServiceType = env("VAN_API_MESSAGE_PAYMENT_SERVICE_TYPE"),
        refundServiceType = env("VAN_API_MESSAGE_REFUND_SERVICE_TYPE"),
        terminalCloseServiceType = env("VAN_API_MESSAGE_TERMINAL_CLOSE_SERVICE_TYPE"),
        terminalCloseFiller = env("VAN_API_MESSAGE_TERMINAL_CLOSE_FILLER"),
        transactionType = env("VAN_API_MESSAGE_TRANSACTION_TYPE"),
        installmentMonths = env("VAN_API_MESSAGE_INSTALLMENT_MONTHS"),
    )
    private val protocolSpec = VanProtocolSpec(
        VanProperties(
            host = "localhost",
            port = 5555,
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
                transactionTimeoutSeconds = env("VAN_API_PROTOCOL_TRANSACTION_TIMEOUT_SECONDS").toLong(),
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

    private fun env(name: String): String {
        return System.getenv(name).orEmpty()
    }
}
