package devcoop.occount.member.application.usecase.login

import devcoop.occount.member.application.exception.InvalidPasswordException
import devcoop.occount.member.application.exception.InvalidPinException
import devcoop.occount.member.application.exception.KioskLoginLockedException
import devcoop.occount.member.application.exception.UserNotFoundException
import devcoop.occount.member.application.support.FakeKioskLoginAttemptRepository
import devcoop.occount.member.application.support.FakePasswordEncoder
import devcoop.occount.member.application.support.FakeTokenGenerator
import devcoop.occount.member.application.support.FakeUserRepository
import devcoop.occount.member.application.support.userFixture
import devcoop.occount.member.domain.user.User
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("LoginUserUseCase 단위 테스트")
class LoginUserUseCaseTest {
    private fun loginUseCase(
        users: List<User> = listOf(userFixture()),
        attemptRepository: FakeKioskLoginAttemptRepository = FakeKioskLoginAttemptRepository(),
    ) = LoginUserUseCase(
        userRepository = FakeUserRepository(users),
        tokenGenerator = FakeTokenGenerator(),
        passwordEncoder = FakePasswordEncoder(),
        kioskLoginAttemptRepository = attemptRepository,
    )

    @Test
    @DisplayName("이메일과 비밀번호가 올바르면 멤버 로그인에 성공하고 액세스 토큰을 반환한다")
    fun `memberLogin succeeds and returns access token`() {
        val result = loginUseCase().login(MemberLoginRequest("test@test.com", "rawPassword"))

        assertEquals("access-1-ROLE_USER", result)
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 멤버 로그인 시 UserNotFoundException이 발생한다")
    fun `memberLogin throws UserNotFoundException when user not found`() {
        assertFailsWith<UserNotFoundException> {
            loginUseCase(users = emptyList()).login(MemberLoginRequest("notfound@test.com", "rawPassword"))
        }
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 멤버 로그인 시 InvalidPasswordException이 발생한다")
    fun `memberLogin throws InvalidPasswordException when password mismatch`() {
        assertFailsWith<InvalidPasswordException> {
            loginUseCase().login(MemberLoginRequest("test@test.com", "wrongPassword"))
        }
    }

    @Test
    @DisplayName("바코드와 핀번호가 올바르면 키오스크 로그인에 성공하고 키오스크 토큰을 반환한다")
    fun `kioskLogin succeeds and returns kiosk token`() {
        val result = loginUseCase().login(KioskLoginRequest("BARCODE123", "123456"))

        assertEquals("kiosk-1-ROLE_USER", result)
    }

    @Test
    @DisplayName("존재하지 않는 바코드로 키오스크 로그인 시 UserNotFoundException이 발생한다")
    fun `kioskLogin throws UserNotFoundException when barcode not found`() {
        assertFailsWith<UserNotFoundException> {
            loginUseCase(users = emptyList()).login(KioskLoginRequest("INVALID_BARCODE", "123456"))
        }
    }

    @Test
    @DisplayName("핀번호가 일치하지 않으면 키오스크 로그인 시 InvalidPinException이 발생한다")
    fun `kioskLogin throws InvalidPinException when pin mismatch`() {
        assertFailsWith<InvalidPinException> {
            loginUseCase().login(KioskLoginRequest("BARCODE123", "wrongPin"))
        }
    }

    @Test
    @DisplayName("핀 실패가 누적되어 잠금 한도에 도달하면 이후 키오스크 로그인은 KioskLoginLockedException이 발생한다")
    fun `kioskLogin locks barcode after repeated pin failures`() {
        val useCase = loginUseCase()

        // 한도(5회)까지 핀 실패 누적
        repeat(5) {
            assertFailsWith<InvalidPinException> {
                useCase.login(KioskLoginRequest("BARCODE123", "wrongPin"))
            }
        }

        // 잠금 후에는 올바른 핀이어도 잠금 예외가 우선한다
        assertFailsWith<KioskLoginLockedException> {
            useCase.login(KioskLoginRequest("BARCODE123", "123456"))
        }
    }

    @Test
    @DisplayName("로그인에 성공하면 누적된 핀 실패 기록이 초기화된다")
    fun `kioskLogin resets failures on success`() {
        val attemptRepository = FakeKioskLoginAttemptRepository()
        val useCase = loginUseCase(attemptRepository = attemptRepository)

        // 한도 직전까지 실패
        repeat(4) {
            assertFailsWith<InvalidPinException> {
                useCase.login(KioskLoginRequest("BARCODE123", "wrongPin"))
            }
        }

        // 성공하면 기록 삭제
        assertEquals("kiosk-1-ROLE_USER", useCase.login(KioskLoginRequest("BARCODE123", "123456")))
        assertEquals(null, attemptRepository.findByBarcode("BARCODE123"))
    }

    @Test
    @DisplayName("한 바코드의 실패는 다른 바코드 로그인에 영향을 주지 않는다")
    fun `kioskLogin failures are isolated per barcode`() {
        val useCase = loginUseCase(
            users = listOf(
                userFixture(id = 1L, email = "a@test.com", barcode = "BARCODE_A"),
                userFixture(id = 2L, email = "b@test.com", barcode = "BARCODE_B"),
            ),
        )

        repeat(5) {
            assertFailsWith<InvalidPinException> {
                useCase.login(KioskLoginRequest("BARCODE_A", "wrongPin"))
            }
        }

        // BARCODE_A가 잠겨도 BARCODE_B는 정상 로그인된다
        assertEquals("kiosk-2-ROLE_USER", useCase.login(KioskLoginRequest("BARCODE_B", "123456")))
    }
}
