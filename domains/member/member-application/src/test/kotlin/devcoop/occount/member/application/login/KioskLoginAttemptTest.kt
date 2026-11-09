package devcoop.occount.member.application.login

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("KioskLoginAttempt 단위 테스트")
class KioskLoginAttemptTest {
    private val t0: Instant = Instant.parse("2026-06-11T00:00:00Z")

    @Test
    @DisplayName("실패가 누적되면 failCount가 증가한다")
    fun `registerFailure increments fail count`() {
        val attempt = KioskLoginAttempt.initial("BARCODE", now = t0)

        val after = attempt.registerFailure(now = t0.plusSeconds(1))

        assertEquals(1, after.failCount)
        assertFalse(after.isLocked(now = t0.plusSeconds(1)))
    }

    @Test
    @DisplayName("실패가 한도에 도달하면 잠금되고 LOCK_SECONDS 동안 잠긴 상태다")
    fun `locks after reaching max fail count`() {
        var attempt = KioskLoginAttempt.initial("BARCODE", now = t0)
        repeat(KioskLoginAttempt.MAX_FAIL_COUNT) {
            attempt = attempt.registerFailure(now = t0.plusSeconds(1))
        }

        assertTrue(attempt.isLocked(now = t0.plusSeconds(1)))
        assertTrue(attempt.isLocked(now = t0.plusSeconds(KioskLoginAttempt.LOCK_SECONDS)))
        assertFalse(attempt.isLocked(now = t0.plusSeconds(KioskLoginAttempt.LOCK_SECONDS + 1)))
    }

    @Test
    @DisplayName("마지막 실패가 카운트 윈도우를 벗어나면 카운트를 새로 센다")
    fun `resets count when window expired`() {
        val attempt = KioskLoginAttempt("BARCODE", failCount = 4, lockedUntil = null, updatedAt = t0)

        val after = attempt.registerFailure(now = t0.plusSeconds(KioskLoginAttempt.WINDOW_SECONDS + 1))

        assertEquals(1, after.failCount)
        assertFalse(after.isLocked(now = t0.plusSeconds(KioskLoginAttempt.WINDOW_SECONDS + 1)))
    }

    @Test
    @DisplayName("잠금이 풀린 뒤의 실패는 카운트를 새 윈도우로 다시 센다")
    fun `resets count after lock released`() {
        val lockedUntil = t0.plusSeconds(KioskLoginAttempt.LOCK_SECONDS)
        val locked = KioskLoginAttempt("BARCODE", failCount = 5, lockedUntil = lockedUntil, updatedAt = t0)

        val after = locked.registerFailure(now = lockedUntil.plusSeconds(1))

        assertEquals(1, after.failCount)
        assertFalse(after.isLocked(now = lockedUntil.plusSeconds(1)))
    }
}
