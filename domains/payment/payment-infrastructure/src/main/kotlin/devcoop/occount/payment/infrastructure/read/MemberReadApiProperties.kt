package devcoop.occount.payment.infrastructure.member

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("member.read-api")
data class MemberReadApiProperties(
    val url: String,
)
