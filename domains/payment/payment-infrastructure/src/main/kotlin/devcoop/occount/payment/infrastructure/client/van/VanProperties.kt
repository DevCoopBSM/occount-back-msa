package devcoop.occount.payment.infrastructure.client.van

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("van.api")
data class VanProperties(
    val terminals: Map<Int, Terminal>,
    val protocol: Protocol,
    val message: Message,
) {
    data class Terminal(
        val host: String,
        val port: Int,
    )


    data class Protocol(
        val stx: String,
        val etx: String,
        val separator: String,
        val recordSeparator: String,
        val blank: String,
        val ack: String,
        val dle: String,
        val formFeed: String,
        val nak: String,
        val transactionTimeoutSeconds: Long,
    )

    data class Message(
        val paymentServiceType: String,
        val refundServiceType: String,
        val terminalCloseServiceType: String,
        val terminalCloseFiller: String,
        val transactionType: String,
        val installmentMonths: String,
    )
}
