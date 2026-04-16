package devcoop.occount.order.bootstrap.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("order")
data class OrderProperties(
    val timeoutSeconds: Long,
    val asyncTimeoutBufferMillis: Long,
)
