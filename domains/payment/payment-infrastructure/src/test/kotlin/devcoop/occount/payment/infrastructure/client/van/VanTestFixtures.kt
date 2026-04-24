package devcoop.occount.payment.infrastructure.client.van

object VanTestFixtures {
    val properties = VanProperties(
        terminals = mapOf(
            1 to VanProperties.Terminal(host = "localhost", port = 5555),
        ),
        protocol = VanProperties.Protocol(
            stx = "04",
            etx = "05",
            separator = "1f",
            recordSeparator = "1d",
            blank = "21",
            ack = "07",
            dle = "11",
            formFeed = "0e",
            nak = "16",
            transactionTimeoutSeconds = 30L,
        ),
        message = VanProperties.Message(
            paymentServiceType = "1101",
            refundServiceType = "2201",
            terminalCloseServiceType = "9901",
            terminalCloseFiller = "7000700",
            transactionType = "77",
            installmentMonths = "09",
        ),
    )

    val protocolCodes = VanProtocolCodes(
        cancelMessageType = "8899",
        rejectStatusPrefix = "ZX",
        cardInsertKeyword = "TEST_INSERT",
    )

    val protocolSpec: VanProtocolSpec = VanProtocolSpec(properties)
    val recordSeparatorChar: Char = protocolSpec.recordSeparatorChar

    fun messageParser(): VanMessageParser {
        val responseParser = VanResponseParser(protocolSpec, protocolCodes)
        return VanMessageParser(protocolSpec, responseParser)
    }
}
