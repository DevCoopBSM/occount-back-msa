package devcoop.occount.member.application.usecase.password

import devcoop.occount.member.application.exception.EmailNotVerifiedException
import devcoop.occount.member.application.exception.UserNotFoundException
import devcoop.occount.member.application.otp.EmailOtp
import devcoop.occount.member.application.support.FakeEmailOtpRepository
import devcoop.occount.member.application.support.FakePasswordEncoder
import devcoop.occount.member.application.support.FakeUserRepository
import devcoop.occount.member.application.support.userFixture
import devcoop.occount.member.application.support.verifiedEmailOtp
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ChangePasswordUseCase 단위 테스트")
class ChangePasswordUseCaseTest {
    private val email = "test@test.com"
    private val request = ChangePasswordRequest(
        email = email,
        newPassword = "newPassword1234",
    )

    @Test
    @DisplayName("이메일 인증이 완료된 상태에서 비밀번호를 변경하면 인코딩된 비밀번호로 저장하고 OTP를 삭제한다")
    fun `changePassword updates encoded password and deletes otp`() {
        val userRepository = FakeUserRepository(initialUsers = listOf(userFixture(email = email)))
        val emailOtpRepository = FakeEmailOtpRepository(
            initialOtpsByEmail = mapOf(email to verifiedEmailOtp(email = email)),
        )
        val useCase = ChangePasswordUseCase(userRepository, emailOtpRepository, FakePasswordEncoder())

        useCase.changePassword(request)

        assertEquals("encoded:newPassword1234", userRepository.savedUsers.last().getPassword())
        assertNull(emailOtpRepository.findByEmail(email))
    }

    @Test
    @DisplayName("이메일 OTP가 존재하지 않으면 EmailNotVerifiedException을 발생시킨다")
    fun `changePassword throws EmailNotVerifiedException when otp does not exist`() {
        val userRepository = FakeUserRepository(initialUsers = listOf(userFixture(email = email)))
        val useCase = ChangePasswordUseCase(userRepository, FakeEmailOtpRepository(), FakePasswordEncoder())

        assertFailsWith<EmailNotVerifiedException> {
            useCase.changePassword(request)
        }
    }

    @Test
    @DisplayName("이메일 OTP가 인증되지 않았으면 EmailNotVerifiedException을 발생시킨다")
    fun `changePassword throws EmailNotVerifiedException when otp not verified`() {
        val userRepository = FakeUserRepository(initialUsers = listOf(userFixture(email = email)))
        val unverifiedOtp = EmailOtp(
            email = email,
            otpCode = "123456",
            expiresAt = Instant.now().plusSeconds(EmailOtp.OTP_TTL_SECONDS),
            createdAt = Instant.now(),
            verified = false,
        )
        val emailOtpRepository = FakeEmailOtpRepository(initialOtpsByEmail = mapOf(email to unverifiedOtp))
        val useCase = ChangePasswordUseCase(userRepository, emailOtpRepository, FakePasswordEncoder())

        assertFailsWith<EmailNotVerifiedException> {
            useCase.changePassword(request)
        }
    }

    @Test
    @DisplayName("인증은 되었으나 해당 이메일의 유저가 없으면 UserNotFoundException을 발생시킨다")
    fun `changePassword throws UserNotFoundException when user not found`() {
        val emailOtpRepository = FakeEmailOtpRepository(
            initialOtpsByEmail = mapOf(email to verifiedEmailOtp(email = email)),
        )
        val useCase = ChangePasswordUseCase(FakeUserRepository(), emailOtpRepository, FakePasswordEncoder())

        assertFailsWith<UserNotFoundException> {
            useCase.changePassword(request)
        }
    }
}
