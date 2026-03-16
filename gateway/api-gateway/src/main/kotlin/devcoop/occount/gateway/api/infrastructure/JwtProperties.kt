package devcoop.occount.gateway.api.infrastructure

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("jwt")
data class JwtProperties(
    val accessExpirationTime: Long,
    val kioskExpirationTime: Long,
    val prefix: String,
    val header: String,
    val secretKey: String,
)
