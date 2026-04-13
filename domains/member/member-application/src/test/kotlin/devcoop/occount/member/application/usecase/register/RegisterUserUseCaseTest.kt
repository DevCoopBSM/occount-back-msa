package devcoop.occount.member.application.usecase.register

import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.member.application.exception.UserAlreadyExistsException
import devcoop.occount.member.application.output.UserRepository
import devcoop.occount.member.domain.user.*
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.*
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder

@DisplayName("RegisterUserUseCase 단위 테스트")
class RegisterUserUseCaseTest {

    private lateinit var userRepository: UserRepository
    private lateinit var eventPublisher: EventPublisher
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var registerUserUseCase: RegisterUserUseCase

    private val defaultPin = "000000"

    // Kotlin + 순수 Mockito에서 non-null 타입에 any() 매처를 사용하기 위한 헬퍼
    @Suppress("UNCHECKED_CAST")
    private fun <T> anyArg(): T = any<Any>() as T

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        eventPublisher = mock(EventPublisher::class.java)
        passwordEncoder = mock(PasswordEncoder::class.java)
        registerUserUseCase = RegisterUserUseCase(userRepository, eventPublisher, passwordEncoder, defaultPin)
    }

    private val request = MemberRegisterRequest(
        userCiNumber = "CI123456",
        userName = "홍길동",
        userAddress = "서울시 강남구",
        userPhone = "010-1234-5678",
        userEmail = "test@test.com",
        password = "password1234",
    )

    private fun createSavedUser(id: Long = 1L) = User(
        id = id,
        userInfo = UserInfo("홍길동", "010-1234-5678", UserType.STUDENT, null, null),
        accountInfo = AccountInfo("test@test.com", "encoded", Role.ROLE_USER, "encoded"),
        userSensitiveInfo = UserSensitiveInfo("CI123456"),
    )

    @Test
    @DisplayName("올바른 요청으로 회원가입 시 유저를 저장하고 MemberRegisteredEvent를 발행한다")
    fun `register saves user and publishes MemberRegisteredEvent`() {
        val savedUser = createSavedUser(id = 1L)

        `when`(passwordEncoder.encode(anyString())).thenReturn("encoded")
        `when`(userRepository.save(anyArg<User>())).thenReturn(savedUser)

        registerUserUseCase.register(request)

        verify(userRepository).save(anyArg())
        verify(eventPublisher, times(1)).publish(anyArg(), anyArg(), anyArg(), anyArg())
    }

    @Test
    @DisplayName("이메일 중복으로 DataIntegrityViolationException 발생 시 UserAlreadyExistsException으로 변환된다")
    fun `register throws UserAlreadyExistsException when email already exists`() {
        `when`(passwordEncoder.encode(anyString())).thenReturn("encoded")
        `when`(userRepository.save(anyArg<User>()))
            .thenThrow(DataIntegrityViolationException("duplicate"))

        assertThrows(UserAlreadyExistsException::class.java) {
            registerUserUseCase.register(request)
        }
    }
}
