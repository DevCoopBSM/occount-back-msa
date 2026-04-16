package devcoop.occount.payment.infrastructure.client.van

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("van.api")
data class VanProperties(
    val url: String,
    val secret: String,
)
