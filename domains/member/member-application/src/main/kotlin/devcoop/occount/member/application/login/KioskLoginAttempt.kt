package devcoop.occount.member.application.login

import java.time.Instant

/**
 * barcode(계정)별 키오스크 로그인 실패 시도를 집계해 PIN 무차별 대입을 막는다.
 * 공유 단말 특성상 게이트웨이 IP 제한은 정상 사용자를 오탐하므로, brute-force 방어는 이 계정 단위에서 한다.
 *
 * [WINDOW_SECONDS] 안에 [MAX_FAIL_COUNT]회 실패하면 [LOCK_SECONDS] 동안 잠근다.
 * 로그인에 성공하면 기록을 삭제해 카운트를 리셋한다.
 */
data class KioskLoginAttempt(
    val userBarcode: String,
    val failCount: Int = 0,
    val lockedUntil: Instant? = null,
    val updatedAt: Instant,
) {
    fun isLocked(now: Instant = Instant.now()): Boolean =
        lockedUntil != null && now.isBefore(lockedUntil)

    fun registerFailure(now: Instant = Instant.now()): KioskLoginAttempt {
        // 직전 잠금이 풀렸거나 마지막 실패가 카운트 윈도우를 벗어났으면 새 윈도우로 다시 센다.
        val windowExpired = now.isAfter(updatedAt.plusSeconds(WINDOW_SECONDS))
        val lockReleased = lockedUntil != null && !now.isBefore(lockedUntil)
        val nextCount = if (windowExpired || lockReleased) 1 else failCount + 1

        // 잠금 도달(== MAX) 이후의 실패는 isLocked로 먼저 막히거나 lockReleased로 1로 리셋되므로
        // nextCount는 MAX를 초과하지 않는다. 방어적으로 >= 를 둔다.
        return if (nextCount >= MAX_FAIL_COUNT) {
            copy(failCount = nextCount, lockedUntil = now.plusSeconds(LOCK_SECONDS), updatedAt = now)
        } else {
            copy(failCount = nextCount, lockedUntil = null, updatedAt = now)
        }
    }

    companion object {
        const val MAX_FAIL_COUNT = 5
        const val WINDOW_SECONDS = 5 * 60L
        const val LOCK_SECONDS = 5 * 60L

        fun initial(userBarcode: String, now: Instant = Instant.now()): KioskLoginAttempt =
            KioskLoginAttempt(userBarcode = userBarcode, updatedAt = now)
    }
}
