package devcoop.occount.member.application.usecase.password

import devcoop.occount.member.application.exception.OtpExpiredException
import devcoop.occount.member.application.exception.OtpLockedException
import devcoop.occount.member.application.exception.OtpMismatchException
import devcoop.occount.member.application.exception.OtpNotFoundException
import devcoop.occount.member.application.otp.EmailOtp
import devcoop.occount.member.application.otp.OtpPurpose
import devcoop.occount.member.application.support.FakeEmailOtpRepository
import devcoop.occount.member.application.support.FakePasswordEncoder
import devcoop.occount.member.application.support.FakeUserRepository
import devcoop.occount.member.application.support.userFixture
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ChangePasswordUseCase 단위 테스트")
class ChangePasswordUseCaseTest {
    private val email = "test@test.com"
    private val request = ChangePasswordRequest(
        email = email,
        otpCode = "123456",
        newPassword = "newPassword1234",
    )

    private fun resetOtp(
        otpCode: String = "123456",
        expiresAt: Instant = Instant.now().plusSeconds(300),
        failCount: Int = 0,
        purpose: OtpPurpose = OtpPurpose.PASSWORD_RESET,
    ) = EmailOtp(
        email = email,
        otpCode = otpCode,
        expiresAt = expiresAt,
        purpose = purpose,
        failCount = failCount,
        createdAt = Instant.now(),
    )

    @Test
    @DisplayName("OTP 코드가 일치하면 비밀번호를 인코딩해 저장하고 OTP를 삭제한다")
    fun `changes encoded password and deletes otp when code matches`() {
        val userRepository = FakeUserRepository(initialUsers = listOf(userFixture(email = email)))
        val emailOtpRepository = FakeEmailOtpRepository(mapOf(email to resetOtp()))
        val useCase = ChangePasswordUseCase(userRepository, emailOtpRepository, FakePasswordEncoder())

        useCase.changePassword(request)

        assertEquals("encoded:newPassword1234", userRepository.savedUsers.last().getPassword())
        assertNull(emailOtpRepository.findByEmail(email))
    }

    @Test
    @DisplayName("OTP가 없으면 OtpNotFoundException을 던진다")
    fun `throws OtpNotFoundException when otp absent`() {
        val useCase = ChangePasswordUseCase(
            FakeUserRepository(listOf(userFixture(email = email))),
            FakeEmailOtpRepository(),
            FakePasswordEncoder(),
        )

        assertFailsWith<OtpNotFoundException> { useCase.changePassword(request) }
    }

    @Test
    @DisplayName("비밀번호 변경 용도가 아닌 OTP는 사용할 수 없다(OtpNotFoundException)")
    fun `rejects otp with non password-reset purpose`() {
        val emailOtpRepository = FakeEmailOtpRepository(mapOf(email to resetOtp(purpose = OtpPurpose.REGISTER)))
        val useCase = ChangePasswordUseCase(
            FakeUserRepository(listOf(userFixture(email = email))),
            emailOtpRepository,
            FakePasswordEncoder(),
        )

        assertFailsWith<OtpNotFoundException> { useCase.changePassword(request) }
    }

    @Test
    @DisplayName("OTP가 만료되면 삭제하고 OtpExpiredException을 던진다")
    fun `throws OtpExpiredException and deletes when expired`() {
        val emailOtpRepository =
            FakeEmailOtpRepository(mapOf(email to resetOtp(expiresAt = Instant.now().minusSeconds(1))))
        val useCase = ChangePasswordUseCase(
            FakeUserRepository(listOf(userFixture(email = email))),
            emailOtpRepository,
            FakePasswordEncoder(),
        )

        assertFailsWith<OtpExpiredException> { useCase.changePassword(request) }
        assertNull(emailOtpRepository.findByEmail(email))
    }

    @Test
    @DisplayName("실패 횟수를 초과하면 삭제하고 OtpLockedException을 던진다")
    fun `throws OtpLockedException and deletes when locked`() {
        val emailOtpRepository =
            FakeEmailOtpRepository(mapOf(email to resetOtp(failCount = EmailOtp.MAX_FAIL_COUNT)))
        val useCase = ChangePasswordUseCase(
            FakeUserRepository(listOf(userFixture(email = email))),
            emailOtpRepository,
            FakePasswordEncoder(),
        )

        assertFailsWith<OtpLockedException> { useCase.changePassword(request) }
        assertNull(emailOtpRepository.findByEmail(email))
    }

    @Test
    @DisplayName("OTP 코드가 일치하지 않으면 실패 횟수를 증가시키고 OtpMismatchException을 던진다")
    fun `throws OtpMismatchException and increments fail count when code mismatches`() {
        val emailOtpRepository = FakeEmailOtpRepository(mapOf(email to resetOtp(otpCode = "999999")))
        val useCase = ChangePasswordUseCase(
            FakeUserRepository(listOf(userFixture(email = email))),
            emailOtpRepository,
            FakePasswordEncoder(),
        )

        assertFailsWith<OtpMismatchException> { useCase.changePassword(request) }
        assertEquals(1, emailOtpRepository.findByEmail(email)?.failCount)
    }

    @Test
    @DisplayName("인증은 통과했으나 유저가 없으면 가입 여부를 노출하지 않고 OTP만 소비한다(예외 없음)")
    fun `does not reveal account existence when user not found`() {
        val userRepository = FakeUserRepository()
        val emailOtpRepository = FakeEmailOtpRepository(mapOf(email to resetOtp()))
        val useCase = ChangePasswordUseCase(userRepository, emailOtpRepository, FakePasswordEncoder())

        useCase.changePassword(request) // 예외 없이 정상 종료

        assertTrue(userRepository.savedUsers.isEmpty())
        assertNull(emailOtpRepository.findByEmail(email))
    }
}
