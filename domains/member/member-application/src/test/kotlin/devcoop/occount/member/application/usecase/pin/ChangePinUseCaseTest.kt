package devcoop.occount.member.application.usecase.pin

import devcoop.occount.member.application.exception.InvalidPasswordException
import devcoop.occount.member.application.exception.UserNotFoundException
import devcoop.occount.member.application.support.FakePasswordEncoder
import devcoop.occount.member.application.support.FakeUserRepository
import devcoop.occount.member.application.support.userFixture
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ChangePinUseCase 단위 테스트")
class ChangePinUseCaseTest {
    // 애플리케이션 테스트의 userFixture 기본 비밀번호는 "rawPassword"
    private val request = ChangePinRequest(password = "rawPassword", newPin = "4321")

    @Test
    @DisplayName("현재 비밀번호가 일치하면 인코딩된 새 PIN으로 저장한다")
    fun `changes encoded pin when current password matches`() {
        val userRepository = FakeUserRepository(initialUsers = listOf(userFixture(id = 1L)))
        val useCase = ChangePinUseCase(userRepository, FakePasswordEncoder())

        useCase.changePin(userId = 1L, request = request)

        assertEquals("encoded:4321", userRepository.savedUsers.last().getUserPin())
    }

    @Test
    @DisplayName("현재 비밀번호가 일치하지 않으면 InvalidPasswordException을 던진다")
    fun `throws InvalidPasswordException when current password does not match`() {
        val userRepository = FakeUserRepository(initialUsers = listOf(userFixture(id = 1L)))
        val useCase = ChangePinUseCase(userRepository, FakePasswordEncoder())

        assertFailsWith<InvalidPasswordException> {
            useCase.changePin(userId = 1L, request = ChangePinRequest(password = "wrong", newPin = "4321"))
        }
    }

    @Test
    @DisplayName("존재하지 않는 유저면 UserNotFoundException을 던진다")
    fun `throws UserNotFoundException when user not found`() {
        val useCase = ChangePinUseCase(FakeUserRepository(), FakePasswordEncoder())

        assertFailsWith<UserNotFoundException> {
            useCase.changePin(userId = 999L, request = request)
        }
    }
}
