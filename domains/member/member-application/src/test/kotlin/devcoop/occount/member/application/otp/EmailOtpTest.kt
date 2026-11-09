package devcoop.occount.member.application.otp

import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("EmailOtp 도메인 단위 테스트")
class EmailOtpTest {
    private val now = Instant.parse("2026-01-01T00:00:00Z")

    private fun otp(
        expiresAt: Instant = now.plusSeconds(300),
        failCount: Int = 0,
        createdAt: Instant = now,
        verified: Boolean = false,
    ) = EmailOtp(
        email = "test@test.com",
        otpCode = "123456",
        expiresAt = expiresAt,
        verified = verified,
        failCount = failCount,
        createdAt = createdAt,
    )

    @Test
    @DisplayName("만료 시각 이후이면 isExpired는 true다")
    fun `isExpired returns true after expiresAt`() {
        assertTrue(otp(expiresAt = now.minusSeconds(1)).isExpired(now))
    }

    @Test
    @DisplayName("만료 시각 이전이면 isExpired는 false다")
    fun `isExpired returns false before expiresAt`() {
        assertFalse(otp(expiresAt = now.plusSeconds(1)).isExpired(now))
    }

    @Test
    @DisplayName("verify는 verified가 true인 복사본을 반환한다")
    fun `verify marks otp as verified`() {
        assertTrue(otp().verify().verified)
    }

    @Test
    @DisplayName("incrementFailCount는 failCount를 1 증가시킨다")
    fun `incrementFailCount increases fail count`() {
        assertEquals(1, otp(failCount = 0).incrementFailCount().failCount)
    }

    @Test
    @DisplayName("실패 횟수가 최대치 이상이면 isLocked는 true다")
    fun `isLocked returns true at max fail count`() {
        assertTrue(otp(failCount = EmailOtp.MAX_FAIL_COUNT).isLocked())
    }

    @Test
    @DisplayName("실패 횟수가 최대치 미만이면 isLocked는 false다")
    fun `isLocked returns false below max fail count`() {
        assertFalse(otp(failCount = EmailOtp.MAX_FAIL_COUNT - 1).isLocked())
    }

    @Test
    @DisplayName("재발송 쿨다운 이내이면 isRecentlySent는 true다")
    fun `isRecentlySent returns true within cooldown`() {
        assertTrue(otp(createdAt = now).isRecentlySent(now.plusSeconds(EmailOtp.RESEND_COOLDOWN_SECONDS - 1)))
    }

    @Test
    @DisplayName("재발송 쿨다운 이후이면 isRecentlySent는 false다")
    fun `isRecentlySent returns false after cooldown`() {
        assertFalse(otp(createdAt = now).isRecentlySent(now.plusSeconds(EmailOtp.RESEND_COOLDOWN_SECONDS + 1)))
    }
}
