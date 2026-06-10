package devcoop.occount.member.application.usecase.register

import devcoop.occount.member.application.event.MemberRegisteredEvent
import devcoop.occount.member.application.exception.EmailNotVerifiedException
import devcoop.occount.member.application.exception.UserAlreadyExistsException
import devcoop.occount.member.application.otp.EmailOtp
import devcoop.occount.member.application.support.FakeEmailOtpRepository
import devcoop.occount.member.application.support.FakeEventPublisher
import devcoop.occount.member.application.support.FakePasswordEncoder
import devcoop.occount.member.application.support.FakeUserRepository
import devcoop.occount.member.application.support.duplicateUserSaveException
import devcoop.occount.member.application.support.verifiedEmailOtp
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

@DisplayName("RegisterUserUseCase 단위 테스트")
class RegisterUserUseCaseTest {
    private val request = MemberRegisterRequest(
        userCiNumber = "CI123456",
        username = "홍길동",
        userPhone = "010-1234-5678",
        birthDate = LocalDate.of(2000, 1, 15),
        userEmail = "test@test.com",
        password = "password1234",
        pin = "123456",
    )

    @Test
    @DisplayName("올바른 요청으로 회원가입 시 유저를 저장하고 MemberRegisteredEvent를 발행한다")
    fun `register saves user and publishes MemberRegisteredEvent`() {
        val userRepository = FakeUserRepository()
        val eventPublisher = FakeEventPublisher()
        val emailOtpRepository = FakeEmailOtpRepository(
            initialOtpsByEmail = mapOf(
                request.userEmail to verifiedEmailOtp(email = request.userEmail),
            ),
        )
        val registerUserUseCase = RegisterUserUseCase(
            userRepository = userRepository,
            eventPublisher = eventPublisher,
            passwordEncoder = FakePasswordEncoder(),
            emailOtpRepository = emailOtpRepository,
        )

        registerUserUseCase.register(request)

        val savedUser = userRepository.savedUsers.single()
        val publishedEvent = eventPublisher.published.single().payload as MemberRegisteredEvent

        assertEquals(savedUser.getId(), publishedEvent.userId)
        assertNull(savedUser.getUserBarcode())
        assertTrue(savedUser.matchesPassword(request.password) { raw, enc -> enc == "encoded:$raw" })
        assertTrue(savedUser.matchesPin(request.pin) { raw, enc -> enc == "encoded:$raw" })
    }

    @Test
    @DisplayName("이메일 OTP 인증이 완료되지 않으면 EmailNotVerifiedException을 발생시킨다")
    fun `register throws EmailNotVerifiedException when email otp is not verified`() {
        val emailOtpRepository = FakeEmailOtpRepository(
            initialOtpsByEmail = mapOf(
                request.userEmail to verifiedEmailOtp(email = request.userEmail).copy(verified = false),
            ),
        )
        val registerUserUseCase = RegisterUserUseCase(
            userRepository = FakeUserRepository(),
            eventPublisher = FakeEventPublisher(),
            passwordEncoder = FakePasswordEncoder(),
            emailOtpRepository = emailOtpRepository,
        )

        assertFailsWith<EmailNotVerifiedException> {
            registerUserUseCase.register(request)
        }
    }

    @Test
    @DisplayName("이메일 OTP가 존재하지 않으면 EmailNotVerifiedException을 발생시킨다")
    fun `register throws EmailNotVerifiedException when email otp does not exist`() {
        val registerUserUseCase = RegisterUserUseCase(
            userRepository = FakeUserRepository(),
            eventPublisher = FakeEventPublisher(),
            passwordEncoder = FakePasswordEncoder(),
            emailOtpRepository = FakeEmailOtpRepository(),
        )

        assertFailsWith<EmailNotVerifiedException> {
            registerUserUseCase.register(request)
        }
    }

    @Test
    @DisplayName("이메일 OTP가 만료되면 EmailNotVerifiedException을 발생시킨다")
    fun `register throws EmailNotVerifiedException when email otp is expired`() {
        val emailOtpRepository = FakeEmailOtpRepository(
            initialOtpsByEmail = mapOf(
                request.userEmail to EmailOtp(
                    email = request.userEmail,
                    otpCode = "123456",
                    expiresAt = Instant.now().minusSeconds(1),
                    createdAt = Instant.now(),
                    verified = true,
                ),
            ),
        )
        val registerUserUseCase = RegisterUserUseCase(
            userRepository = FakeUserRepository(),
            eventPublisher = FakeEventPublisher(),
            passwordEncoder = FakePasswordEncoder(),
            emailOtpRepository = emailOtpRepository,
        )

        assertFailsWith<EmailNotVerifiedException> {
            registerUserUseCase.register(request)
        }
    }

    @Test
    @DisplayName("이메일 중복으로 DataIntegrityViolationException 발생 시 UserAlreadyExistsException으로 변환된다")
    fun `register throws UserAlreadyExistsException when email already exists`() {
        val emailOtpRepository = FakeEmailOtpRepository(
            initialOtpsByEmail = mapOf(
                request.userEmail to verifiedEmailOtp(email = request.userEmail),
            ),
        )
        val registerUserUseCase = RegisterUserUseCase(
            userRepository = FakeUserRepository(saveException = duplicateUserSaveException()),
            eventPublisher = FakeEventPublisher(),
            passwordEncoder = FakePasswordEncoder(),
            emailOtpRepository = emailOtpRepository,
        )

        assertFailsWith<UserAlreadyExistsException> {
            registerUserUseCase.register(request)
        }
    }

}
