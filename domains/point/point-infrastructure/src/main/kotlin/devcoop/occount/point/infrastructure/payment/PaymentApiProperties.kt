package devcoop.occount.point.infrastructure.payment

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("payment.api")
data class PaymentApiProperties(
    val url: String,
)
