package devcoop.occount.member.application.usecase.register

import devcoop.occount.member.application.event.MemberRegisteredEvent
import devcoop.occount.member.application.exception.UserAlreadyExistsException
import devcoop.occount.member.application.support.FakeEventPublisher
import devcoop.occount.member.application.support.FakePasswordEncoder
import devcoop.occount.member.application.support.FakeUserRepository
import devcoop.occount.member.application.support.duplicateUserSaveException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("RegisterUserUseCase 단위 테스트")
class RegisterUserUseCaseTest {
    private val defaultPin = "000000"

    private val request = MemberRegisterRequest(
        userCiNumber = "CI123456",
        userName = "홍길동",
        userPhone = "010-1234-5678",
        userEmail = "test@test.com",
        password = "password1234",
    )

    @Test
    @DisplayName("올바른 요청으로 회원가입 시 유저를 저장하고 MemberRegisteredEvent를 발행한다")
    fun `register saves user and publishes MemberRegisteredEvent`() {
        val userRepository = FakeUserRepository()
        val eventPublisher = FakeEventPublisher()
        val registerUserUseCase = RegisterUserUseCase(
            userRepository = userRepository,
            eventPublisher = eventPublisher,
            passwordEncoder = FakePasswordEncoder(),
            defaultPin = defaultPin,
        )

        registerUserUseCase.register(request)

        val savedUser = userRepository.savedUsers.single()
        val publishedEvent = eventPublisher.published.single().payload as MemberRegisteredEvent

        assertEquals(savedUser.getId(), publishedEvent.userId)
        assertTrue(savedUser.matchesPassword(request.password) { raw, enc -> enc == "encoded:$raw" })
        assertTrue(savedUser.matchesPin(defaultPin) { raw, enc -> enc == "encoded:$raw" })
    }

    @Test
    @DisplayName("이메일 중복으로 DataIntegrityViolationException 발생 시 UserAlreadyExistsException으로 변환된다")
    fun `register throws UserAlreadyExistsException when email already exists`() {
        val registerUserUseCase = RegisterUserUseCase(
            userRepository = FakeUserRepository(saveException = duplicateUserSaveException()),
            eventPublisher = FakeEventPublisher(),
            passwordEncoder = FakePasswordEncoder(),
            defaultPin = defaultPin,
        )

        assertFailsWith<UserAlreadyExistsException> {
            registerUserUseCase.register(request)
        }
    }
}
