package devcoop.occount.payment.infrastructure.watchdog

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("alerts")
data class AlertProperties(
    val webhookUrl: String? = null,
)
