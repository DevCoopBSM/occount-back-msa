package devcoop.occount.payment.infrastructure.client.van

import kotlin.test.Test
import kotlin.test.assertEquals

class VanMessageBuilderTest {
    private val properties = VanProperties(
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
    )
    private val builder = VanMessageBuilder(
        protocolSpec = VanProtocolSpec(properties),
        properties = properties,
    )

    @Test
    fun `무카드 취소 전문을 2101 규격으로 조립한다`() {
        val message = builder.buildCardlessCancelMessage(
            amount = 1000,
            approvalDate = "260420",
            terminalSequence = "3358",
            approvalNumber = "71060497",
        )

        assertEquals(68, message.size)
        assertEquals("0068", message.copyOfRange(1, 5).toString(Charsets.US_ASCII))
        assertEquals(
            "2101\u001c1000\u001c260420\u001c71060497    \u001c02026CANC260420335871060497    \u0003",
            message.copyOfRange(5, message.size - 1).toString(Charsets.US_ASCII),
        )
    }
}
