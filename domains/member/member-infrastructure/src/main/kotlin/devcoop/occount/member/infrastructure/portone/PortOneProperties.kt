package devcoop.occount.member.infrastructure.portone

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "portone")
data class PortOneProperties(
    val secretKey: String,
)
