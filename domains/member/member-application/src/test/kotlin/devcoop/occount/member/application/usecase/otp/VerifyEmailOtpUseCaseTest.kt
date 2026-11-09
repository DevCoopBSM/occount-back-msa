package devcoop.occount.member.application.usecase.otp

import devcoop.occount.member.application.exception.OtpExpiredException
import devcoop.occount.member.application.exception.OtpLockedException
import devcoop.occount.member.application.exception.OtpMismatchException
import devcoop.occount.member.application.exception.OtpNotFoundException
import devcoop.occount.member.application.otp.EmailOtp
import devcoop.occount.member.application.support.FakeEmailOtpRepository
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("VerifyEmailOtpUseCase 단위 테스트")
class VerifyEmailOtpUseCaseTest {
    private val email = "test@test.com"

    private fun otp(
        otpCode: String = "123456",
        expiresAt: Instant = Instant.now().plusSeconds(300),
        failCount: Int = 0,
        verified: Boolean = false,
    ) = EmailOtp(
        email = email,
        otpCode = otpCode,
        expiresAt = expiresAt,
        verified = verified,
        failCount = failCount,
        createdAt = Instant.now(),
    )

    @Test
    @DisplayName("OTP가 없으면 OtpNotFoundException을 던진다")
    fun `throws OtpNotFoundException when otp not found`() {
        val useCase = VerifyEmailOtpUseCase(FakeEmailOtpRepository())

        assertFailsWith<OtpNotFoundException> { useCase.verify(email, "123456") }
    }

    @Test
    @DisplayName("OTP가 만료되면 삭제하고 OtpExpiredException을 던진다")
    fun `deletes and throws OtpExpiredException when expired`() {
        val repository = FakeEmailOtpRepository(mapOf(email to otp(expiresAt = Instant.now().minusSeconds(1))))
        val useCase = VerifyEmailOtpUseCase(repository)

        assertFailsWith<OtpExpiredException> { useCase.verify(email, "123456") }
        assertNull(repository.findByEmail(email))
    }

    @Test
    @DisplayName("실패 횟수를 초과하면 삭제하고 OtpLockedException을 던진다")
    fun `deletes and throws OtpLockedException when locked`() {
        val repository = FakeEmailOtpRepository(mapOf(email to otp(failCount = EmailOtp.MAX_FAIL_COUNT)))
        val useCase = VerifyEmailOtpUseCase(repository)

        assertFailsWith<OtpLockedException> { useCase.verify(email, "123456") }
        assertNull(repository.findByEmail(email))
    }

    @Test
    @DisplayName("코드가 일치하지 않으면 실패 횟수를 증가시키고 OtpMismatchException을 던진다")
    fun `increments fail count and throws OtpMismatchException when code mismatches`() {
        val repository = FakeEmailOtpRepository(mapOf(email to otp(otpCode = "123456")))
        val useCase = VerifyEmailOtpUseCase(repository)

        assertFailsWith<OtpMismatchException> { useCase.verify(email, "999999") }
        assertEquals(1, repository.findByEmail(email)?.failCount)
    }

    @Test
    @DisplayName("코드가 일치하면 verified 상태로 저장한다")
    fun `marks otp verified when code matches`() {
        val repository = FakeEmailOtpRepository(mapOf(email to otp(otpCode = "123456")))
        val useCase = VerifyEmailOtpUseCase(repository)

        useCase.verify(email, "123456")

        assertTrue(repository.findByEmail(email)!!.verified)
    }
}
