package devcoop.occount.member.application.usecase.pin

import devcoop.occount.member.application.exception.PinChangeTicketExpiredException
import devcoop.occount.member.application.exception.PinChangeTicketNotFoundException
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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ChangePinUseCase 단위 테스트")
class ChangePinUseCaseTest {
    private fun validTicket(token: String = "ticket-1", userId: Long = 1L) = PinChangeTicket(
        token = token,
        userId = userId,
        expiresAt = Instant.now().plusSeconds(PinChangeTicket.TTL_SECONDS),
        createdAt = Instant.now(),
    )

    @Test
    @DisplayName("유효한 티켓이면 인코딩된 새 PIN으로 저장하고 티켓을 소비한다")
    fun `changes encoded pin and consumes ticket when ticket is valid`() {
        val userRepository = FakeUserRepository(initialUsers = listOf(userFixture(id = 1L)))
        val ticketRepository = FakePinChangeTicketRepository(initialTickets = listOf(validTicket()))
        val useCase = ChangePinUseCase(userRepository, ticketRepository, FakePasswordEncoder())

        useCase.changePin(userId = 1L, request = ChangePinRequest(ticket = "ticket-1", newPin = "4321"))

        assertEquals("encoded:4321", userRepository.savedUsers.last().getUserPin())
        assertNull(ticketRepository.findByToken("ticket-1"))
    }

    @Test
    @DisplayName("티켓이 존재하지 않으면 PinChangeTicketNotFoundException을 던진다")
    fun `throws PinChangeTicketNotFoundException when ticket not found`() {
        val userRepository = FakeUserRepository(initialUsers = listOf(userFixture(id = 1L)))
        val useCase = ChangePinUseCase(userRepository, FakePinChangeTicketRepository(), FakePasswordEncoder())

        assertFailsWith<PinChangeTicketNotFoundException> {
            useCase.changePin(userId = 1L, request = ChangePinRequest(ticket = "unknown", newPin = "4321"))
        }
    }

    @Test
    @DisplayName("티켓이 다른 사용자 소유면 PinChangeTicketNotFoundException을 던진다")
    fun `throws PinChangeTicketNotFoundException when ticket belongs to another user`() {
        val userRepository = FakeUserRepository(initialUsers = listOf(userFixture(id = 1L)))
        val ticketRepository = FakePinChangeTicketRepository(
            initialTickets = listOf(validTicket(token = "ticket-2", userId = 2L)),
        )
        val useCase = ChangePinUseCase(userRepository, ticketRepository, FakePasswordEncoder())

        assertFailsWith<PinChangeTicketNotFoundException> {
            useCase.changePin(userId = 1L, request = ChangePinRequest(ticket = "ticket-2", newPin = "4321"))
        }
    }

    @Test
    @DisplayName("티켓이 만료되었으면 폐기하고 PinChangeTicketExpiredException을 던진다")
    fun `throws PinChangeTicketExpiredException and deletes ticket when expired`() {
        val userRepository = FakeUserRepository(initialUsers = listOf(userFixture(id = 1L)))
        val expiredTicket = PinChangeTicket(
            token = "ticket-1",
            userId = 1L,
            expiresAt = Instant.now().minusSeconds(1),
            createdAt = Instant.now().minusSeconds(PinChangeTicket.TTL_SECONDS),
        )
        val ticketRepository = FakePinChangeTicketRepository(initialTickets = listOf(expiredTicket))
        val useCase = ChangePinUseCase(userRepository, ticketRepository, FakePasswordEncoder())

        assertFailsWith<PinChangeTicketExpiredException> {
            useCase.changePin(userId = 1L, request = ChangePinRequest(ticket = "ticket-1", newPin = "4321"))
        }
        assertNull(ticketRepository.findByToken("ticket-1"))
    }

    @Test
    @DisplayName("존재하지 않는 유저면 UserNotFoundException을 던진다")
    fun `throws UserNotFoundException when user not found`() {
        val ticketRepository = FakePinChangeTicketRepository(
            initialTickets = listOf(validTicket(userId = 999L)),
        )
        val useCase = ChangePinUseCase(FakeUserRepository(), ticketRepository, FakePasswordEncoder())

        assertFailsWith<UserNotFoundException> {
            useCase.changePin(userId = 999L, request = ChangePinRequest(ticket = "ticket-1", newPin = "4321"))
        }
    }
}
