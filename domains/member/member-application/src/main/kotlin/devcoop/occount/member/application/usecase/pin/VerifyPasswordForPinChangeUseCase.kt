package devcoop.occount.member.application.usecase.pin

import devcoop.occount.member.application.exception.InvalidPasswordException
import devcoop.occount.member.application.exception.UserNotFoundException
import devcoop.occount.member.application.output.PinChangeTicketRepository
import devcoop.occount.member.application.output.UserRepository
import devcoop.occount.member.application.pin.PinChangeTicket
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64

/**
 * PIN 변경 1단계: 현재 비밀번호만 검증하고, 통과 시 새 PIN 제출에 쓸 1회용 티켓을 발급한다.
 * 직전에 발급된 티켓은 폐기해 사용자당 유효 티켓이 하나만 존재하도록 한다.
 */
@Service
class VerifyPasswordForPinChangeUseCase(
    private val userRepository: UserRepository,
    private val pinChangeTicketRepository: PinChangeTicketRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    private val secureRandom = SecureRandom()

    @Transactional
    fun verify(userId: Long, request: VerifyPasswordForPinChangeRequest): PinChangeTicketResponse {
        val user = userRepository.findById(userId)
            ?: throw UserNotFoundException()

        val matches = user.matchesPassword(request.password) { raw, encoded ->
            passwordEncoder.matches(raw, encoded)
        }
        if (!matches) {
            throw InvalidPasswordException()
        }

        pinChangeTicketRepository.deleteByUserId(userId)

        val now = Instant.now()
        val ticket = pinChangeTicketRepository.save(
            PinChangeTicket(
                token = generateToken(),
                userId = userId,
                expiresAt = now.plusSeconds(PinChangeTicket.TTL_SECONDS),
                createdAt = now,
            )
        )

        return PinChangeTicketResponse(
            ticket = ticket.token,
            expiresIn = PinChangeTicket.TTL_SECONDS,
        )
    }

    private fun generateToken(): String {
        val bytes = ByteArray(TOKEN_BYTE_LENGTH)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    companion object {
        private const val TOKEN_BYTE_LENGTH = 32
    }
}
