package devcoop.occount.payment.infrastructure.client.van

import kotlin.test.Test
import kotlin.test.assertEquals

class VanMessageBuilderTest {
    private val properties = VanProperties(
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
    )
    private val builder = VanMessageBuilder(
        protocolSpec = VanProtocolSpec(properties),
        properties = properties,
    )

    @Test
    fun `무카드 취소 전문을 refund 규격으로 조립한다`() {
        val message = builder.buildCardlessCancelMessage(
            amount = 1000,
            approvalDate = "260420",
            terminalSequence = "3358",
            approvalNumber = "71060497",
        )

        val protocolSpec = VanProtocolSpec(properties)
        assertEquals(68, message.size)
        assertEquals("0068", message.copyOfRange(1, 5).toString(Charsets.US_ASCII))
        assertEquals(
            "${properties.message.refundServiceType}${protocolSpec.separatorChar}1000${protocolSpec.separatorChar}260420${protocolSpec.separatorChar}71060497    ${protocolSpec.separatorChar}02026CANC260420335871060497    ${Char(protocolSpec.etxByte.toInt())}",
            message.copyOfRange(5, message.size - 1).toString(Charsets.US_ASCII),
        )
    }

    private fun env(name: String): String = System.getenv(name).orEmpty()
}
