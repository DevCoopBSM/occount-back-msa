package devcoop.occount.member.application.usecase.login

import devcoop.occount.member.application.exception.InvalidPasswordException
import devcoop.occount.member.application.exception.UserNotFoundException
import devcoop.occount.member.application.output.TokenGenerator
import devcoop.occount.member.application.output.UserRepository
import devcoop.occount.member.domain.user.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.security.crypto.password.PasswordEncoder

@DisplayName("LoginUserUseCase 단위 테스트")
class LoginUserUseCaseTest {

    private lateinit var userRepository: UserRepository
    private lateinit var tokenGenerator: TokenGenerator
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var loginUserUseCase: LoginUserUseCase

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        tokenGenerator = mock(TokenGenerator::class.java)
        passwordEncoder = mock(PasswordEncoder::class.java)
        loginUserUseCase = LoginUserUseCase(userRepository, tokenGenerator, passwordEncoder)
    }

    private fun createUser(id: Long = 1L, role: Role = Role.ROLE_USER) = User(
        id = id,
        userInfo = UserInfo("홍길동", "010-1234-5678", UserType.STUDENT, null, "BARCODE123"),
        accountInfo = AccountInfo("test@test.com", "encodedPassword", role, "encodedPin"),
        userSensitiveInfo = UserSensitiveInfo("CI123"),
    )

    @Test
    @DisplayName("이메일과 비밀번호가 올바르면 멤버 로그인에 성공하고 액세스 토큰을 반환한다")
    fun `memberLogin succeeds and returns access token`() {
        val user = createUser()
        val request = MemberLoginRequest("test@test.com", "rawPassword")

        `when`(userRepository.findByUserEmail("test@test.com")).thenReturn(user)
        `when`(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(true)
        `when`(tokenGenerator.createAccessToken(1L, "ROLE_USER")).thenReturn("accessToken")

        val result = loginUserUseCase.login(request)

        assertEquals("accessToken", result)
        verify(tokenGenerator).createAccessToken(1L, "ROLE_USER")
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 멤버 로그인 시 UserNotFoundException이 발생한다")
    fun `memberLogin throws UserNotFoundException when user not found`() {
        val request = MemberLoginRequest("notfound@test.com", "rawPassword")
        `when`(userRepository.findByUserEmail("notfound@test.com")).thenReturn(null)

        assertThrows(UserNotFoundException::class.java) {
            loginUserUseCase.login(request)
        }
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 멤버 로그인 시 InvalidPasswordException이 발생한다")
    fun `memberLogin throws InvalidPasswordException when password mismatch`() {
        val user = createUser()
        val request = MemberLoginRequest("test@test.com", "wrongPassword")

        `when`(userRepository.findByUserEmail("test@test.com")).thenReturn(user)
        `when`(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false)

        assertThrows(InvalidPasswordException::class.java) {
            loginUserUseCase.login(request)
        }
    }

    @Test
    @DisplayName("바코드와 핀번호가 올바르면 키오스크 로그인에 성공하고 키오스크 토큰을 반환한다")
    fun `kioskLogin succeeds and returns kiosk token`() {
        val user = createUser()
        val request = KioskLoginRequest("BARCODE123", "rawPin")

        `when`(userRepository.findByUserBarcode("BARCODE123")).thenReturn(user)
        `when`(passwordEncoder.matches("rawPin", "encodedPin")).thenReturn(true)
        `when`(tokenGenerator.createKioskToken(1L, "ROLE_USER")).thenReturn("kioskToken")

        val result = loginUserUseCase.login(request)

        assertEquals("kioskToken", result)
        verify(tokenGenerator).createKioskToken(1L, "ROLE_USER")
    }

    @Test
    @DisplayName("존재하지 않는 바코드로 키오스크 로그인 시 UserNotFoundException이 발생한다")
    fun `kioskLogin throws UserNotFoundException when barcode not found`() {
        val request = KioskLoginRequest("INVALID_BARCODE", "rawPin")
        `when`(userRepository.findByUserBarcode("INVALID_BARCODE")).thenReturn(null)

        assertThrows(UserNotFoundException::class.java) {
            loginUserUseCase.login(request)
        }
    }

    @Test
    @DisplayName("핀번호가 일치하지 않으면 키오스크 로그인 시 InvalidPasswordException이 발생한다")
    fun `kioskLogin throws InvalidPasswordException when pin mismatch`() {
        val user = createUser()
        val request = KioskLoginRequest("BARCODE123", "wrongPin")

        `when`(userRepository.findByUserBarcode("BARCODE123")).thenReturn(user)
        `when`(passwordEncoder.matches("wrongPin", "encodedPin")).thenReturn(false)

        assertThrows(InvalidPasswordException::class.java) {
            loginUserUseCase.login(request)
        }
    }

    @Test
    @DisplayName("비밀번호가 일치하면 passwordValidate는 예외를 발생시키지 않는다")
    fun `passwordValidate does not throw when passwords match`() {
        `when`(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(true)

        assertDoesNotThrow {
            loginUserUseCase.passwordValidate("rawPassword", "encodedPassword")
        }
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 passwordValidate는 InvalidPasswordException을 발생시킨다")
    fun `passwordValidate throws InvalidPasswordException when passwords do not match`() {
        `when`(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false)

        assertThrows(InvalidPasswordException::class.java) {
            loginUserUseCase.passwordValidate("wrongPassword", "encodedPassword")
        }
    }
}
