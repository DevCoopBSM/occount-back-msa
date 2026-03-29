package devcoop.occount.payment.infrastructure.point

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("point.api")
data class PointApiProperties(
    val url: String,
)
