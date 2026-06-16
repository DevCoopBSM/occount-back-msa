package devcoop.occount.investment.infrastructure.client

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * @property secretKey PortOne REST API 인증 키(Authorization: "PortOne {secretKey}").
 * @property webhookSecret PortOne 웹훅 서명 검증 시크릿(whsec_ 로 시작).
 */
@ConfigurationProperties(prefix = "portone")
data class PortOneProperties(
    val secretKey: String = "",
    val webhookSecret: String = "",
)
