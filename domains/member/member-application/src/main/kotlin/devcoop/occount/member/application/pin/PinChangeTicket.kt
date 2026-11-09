package devcoop.occount.member.application.pin

import java.time.Instant

/**
 * 비밀번호 확인을 통과한 사용자가 새 PIN을 제출할 때까지 단계를 잇는 1회용 티켓.
 * 발급 시점의 사용자에게만 바인딩되며, 짧은 만료 시간을 가지고 소비되면 즉시 폐기된다.
 */
data class PinChangeTicket(
    val token: String,
    val userId: Long,
    val expiresAt: Instant,
    val createdAt: Instant,
) {
    fun isExpired(now: Instant = Instant.now()): Boolean = now.isAfter(expiresAt)

    fun isOwnedBy(userId: Long): Boolean = this.userId == userId

    companion object {
        const val TTL_SECONDS = 5 * 60L
    }
}
