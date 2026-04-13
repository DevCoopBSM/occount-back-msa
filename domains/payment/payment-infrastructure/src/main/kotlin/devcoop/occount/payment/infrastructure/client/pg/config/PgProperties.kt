package devcoop.occount.payment.infrastructure.client.pg.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("payment.pg")
data class PgProperties(
    val host: String,
    val port: Int,
    val connectTimeoutMillis: Int = 5_000,
    val readTimeoutMillis: Int = 1_000,
    val retryDelayMillis: Long = 5_000,
    val maxRetries: Int = 3,
    val transactionTimeoutMillis: Long = 30_000,
)
