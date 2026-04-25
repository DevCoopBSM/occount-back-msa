package devcoop.occount.member.application.usecase.login

import devcoop.occount.member.application.exception.InvalidPasswordException
import devcoop.occount.member.application.exception.InvalidPinException
import devcoop.occount.member.application.exception.UserNotFoundException
import devcoop.occount.member.application.support.FakePasswordEncoder
import devcoop.occount.member.application.support.FakeTokenGenerator
import devcoop.occount.member.application.support.FakeUserRepository
import devcoop.occount.member.application.support.userFixture
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("LoginUserUseCase 단위 테스트")
class LoginUserUseCaseTest {
    @Test
    @DisplayName("이메일과 비밀번호가 올바르면 멤버 로그인에 성공하고 액세스 토큰을 반환한다")
    fun `memberLogin succeeds and returns access token`() {
        val loginUserUseCase = LoginUserUseCase(
            userRepository = FakeUserRepository(listOf(userFixture())),
            tokenGenerator = FakeTokenGenerator(),
            passwordEncoder = FakePasswordEncoder(),
        )

        val result = loginUserUseCase.login(
            MemberLoginRequest("test@test.com", "rawPassword"),
        )

        assertEquals("access-1-ROLE_USER", result)
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 멤버 로그인 시 UserNotFoundException이 발생한다")
    fun `memberLogin throws UserNotFoundException when user not found`() {
        val loginUserUseCase = LoginUserUseCase(
            userRepository = FakeUserRepository(),
            tokenGenerator = FakeTokenGenerator(),
            passwordEncoder = FakePasswordEncoder(),
        )

        assertFailsWith<UserNotFoundException> {
            loginUserUseCase.login(MemberLoginRequest("notfound@test.com", "rawPassword"))
        }
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 멤버 로그인 시 InvalidPasswordException이 발생한다")
    fun `memberLogin throws InvalidPasswordException when password mismatch`() {
        val loginUserUseCase = LoginUserUseCase(
            userRepository = FakeUserRepository(listOf(userFixture())),
            tokenGenerator = FakeTokenGenerator(),
            passwordEncoder = FakePasswordEncoder(),
        )

        assertFailsWith<InvalidPasswordException> {
            loginUserUseCase.login(MemberLoginRequest("test@test.com", "wrongPassword"))
        }
    }

    @Test
    @DisplayName("바코드와 핀번호가 올바르면 키오스크 로그인에 성공하고 키오스크 토큰을 반환한다")
    fun `kioskLogin succeeds and returns kiosk token`() {
        val loginUserUseCase = LoginUserUseCase(
            userRepository = FakeUserRepository(listOf(userFixture())),
            tokenGenerator = FakeTokenGenerator(),
            passwordEncoder = FakePasswordEncoder(),
        )

        val result = loginUserUseCase.login(
            KioskLoginRequest("BARCODE123", "123456"),
        )

        assertEquals("kiosk-1-ROLE_USER", result)
    }

    @Test
    @DisplayName("존재하지 않는 바코드로 키오스크 로그인 시 UserNotFoundException이 발생한다")
    fun `kioskLogin throws UserNotFoundException when barcode not found`() {
        val loginUserUseCase = LoginUserUseCase(
            userRepository = FakeUserRepository(),
            tokenGenerator = FakeTokenGenerator(),
            passwordEncoder = FakePasswordEncoder(),
        )

        assertFailsWith<UserNotFoundException> {
            loginUserUseCase.login(KioskLoginRequest("INVALID_BARCODE", "123456"))
        }
    }

    @Test
    @DisplayName("핀번호가 일치하지 않으면 키오스크 로그인 시 InvalidPinException이 발생한다")
    fun `kioskLogin throws InvalidPinException when pin mismatch`() {
        val loginUserUseCase = LoginUserUseCase(
            userRepository = FakeUserRepository(listOf(userFixture())),
            tokenGenerator = FakeTokenGenerator(),
            passwordEncoder = FakePasswordEncoder(),
        )

        assertFailsWith<InvalidPinException> {
            loginUserUseCase.login(KioskLoginRequest("BARCODE123", "wrongPin"))
        }
    }
}
