package devcoop.occount.member.application.usecase.pin

import devcoop.occount.member.application.exception.PinChangeTicketExpiredException
import devcoop.occount.member.application.exception.PinChangeTicketNotFoundException
import devcoop.occount.member.application.exception.UserNotFoundException
import devcoop.occount.member.application.output.PinChangeTicketRepository
import devcoop.occount.member.application.output.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * PIN 변경 2단계: 1단계에서 발급된 1회용 티켓을 검증하고 새 PIN으로 변경한다.
 * 티켓은 발급받은 사용자 본인에게만 유효하며, 검증 통과 시 즉시 소비(폐기)된다.
 */
@Service
class ChangePinUseCase(
    private val userRepository: UserRepository,
    private val pinChangeTicketRepository: PinChangeTicketRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    @Transactional
    fun changePin(userId: Long, request: ChangePinRequest) {
        // 티켓이 다른 사용자 소유여도 존재 여부를 노출하지 않도록 동일하게 NotFound로 처리한다.
        val ticket = pinChangeTicketRepository.findByToken(request.ticket)
            ?.takeIf { it.isOwnedBy(userId) }
            ?: throw PinChangeTicketNotFoundException()

        if (ticket.isExpired()) {
            pinChangeTicketRepository.deleteByToken(ticket.token)
            throw PinChangeTicketExpiredException()
        }

        val user = userRepository.findById(userId)
            ?: throw UserNotFoundException()

        userRepository.save(user.changePin(passwordEncoder.encode(request.newPin)!!))

        pinChangeTicketRepository.deleteByToken(ticket.token)
    }
}
