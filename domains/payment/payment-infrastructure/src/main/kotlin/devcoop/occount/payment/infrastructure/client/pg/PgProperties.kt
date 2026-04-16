package devcoop.occount.payment.infrastructure.client.pg

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("payment.api")
data class PgProperties(
    val url: String,
    val secret: String,
)
