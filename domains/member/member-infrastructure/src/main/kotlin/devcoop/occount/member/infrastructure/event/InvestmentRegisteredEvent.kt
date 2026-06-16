package devcoop.occount.member.infrastructure.event

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * investment 도메인이 발행하는 출자 등록 이벤트의 소비 측 사본.
 * 승격에는 userId 만 필요하므로 나머지 필드는 무시한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class InvestmentRegisteredEvent(
    val userId: Long = 0L,
)
