package devcoop.occount.member.application.usecase.pin

import devcoop.occount.member.application.exception.InvalidPasswordException
import devcoop.occount.member.application.exception.UserNotFoundException
import devcoop.occount.member.application.pin.PinChangeTicket
import devcoop.occount.member.application.support.FakePasswordEncoder
import devcoop.occount.member.application.support.FakePinChangeTicketRepository
import devcoop.occount.member.application.support.FakeUserRepository
import devcoop.occount.member.application.support.userFixture
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("VerifyPasswordForPinChangeUseCase 단위 테스트")
class VerifyPasswordForPinChangeUseCaseTest {
    // 애플리케이션 테스트의 userFixture 기본 비밀번호는 "rawPassword"
    private val request = VerifyPasswordForPinChangeRequest(password = "rawPassword")

    @Test
    @DisplayName("비밀번호가 일치하면 만료시간이 설정된 티켓을 발급한다")
    fun `issues ticket when password matches`() {
        val userRepository = FakeUserRepository(initialUsers = listOf(userFixture(id = 1L)))
        val ticketRepository = FakePinChangeTicketRepository()
        val useCase = VerifyPasswordForPinChangeUseCase(userRepository, ticketRepository, FakePasswordEncoder())

        val response = useCase.verify(userId = 1L, request = request)

        assertEquals(PinChangeTicket.TTL_SECONDS, response.expiresIn)
        assertTrue(response.ticket.isNotBlank())
        val saved = ticketRepository.findByToken(response.ticket)!!
        assertEquals(1L, saved.userId)
        assertTrue(saved.expiresAt.isAfter(Instant.now()))
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 InvalidPasswordException을 던지고 티켓을 발급하지 않는다")
    fun `throws InvalidPasswordException when password does not match`() {
        val userRepository = FakeUserRepository(initialUsers = listOf(userFixture(id = 1L)))
        val ticketRepository = FakePinChangeTicketRepository()
        val useCase = VerifyPasswordForPinChangeUseCase(userRepository, ticketRepository, FakePasswordEncoder())

        assertFailsWith<InvalidPasswordException> {
            useCase.verify(userId = 1L, request = VerifyPasswordForPinChangeRequest(password = "wrong"))
        }
        assertTrue(ticketRepository.savedTickets.isEmpty())
    }

    @Test
    @DisplayName("존재하지 않는 유저면 UserNotFoundException을 던진다")
    fun `throws UserNotFoundException when user not found`() {
        val useCase = VerifyPasswordForPinChangeUseCase(
            FakeUserRepository(),
            FakePinChangeTicketRepository(),
            FakePasswordEncoder(),
        )

        assertFailsWith<UserNotFoundException> {
            useCase.verify(userId = 999L, request = request)
        }
    }

    @Test
    @DisplayName("직전에 발급된 티켓은 폐기하고 새 티켓만 유효하게 한다")
    fun `invalidates previously issued ticket`() {
        val userRepository = FakeUserRepository(initialUsers = listOf(userFixture(id = 1L)))
        val oldTicket = PinChangeTicket(
            token = "old-ticket",
            userId = 1L,
            expiresAt = Instant.now().plusSeconds(PinChangeTicket.TTL_SECONDS),
            createdAt = Instant.now(),
        )
        val ticketRepository = FakePinChangeTicketRepository(initialTickets = listOf(oldTicket))
        val useCase = VerifyPasswordForPinChangeUseCase(userRepository, ticketRepository, FakePasswordEncoder())

        val response = useCase.verify(userId = 1L, request = request)

        assertNull(ticketRepository.findByToken("old-ticket"))
        assertEquals(1L, ticketRepository.findByToken(response.ticket)!!.userId)
    }
}
