package devcoop.occount.member.application.usecase.register

import devcoop.occount.core.common.event.DomainEventTypes
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.member.application.event.MemberRegisteredEvent
import devcoop.occount.member.application.exception.UserAlreadyExistsException
import devcoop.occount.member.application.output.UserRepository
import devcoop.occount.member.domain.user.User
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegisterUserUseCase(
    private val userRepository: UserRepository,
    private val eventPublisher: EventPublisher,
    private val passwordEncoder: PasswordEncoder,
    @param:Value("\${app.default-pin}")
    private val defaultPin: String,
) {
    @Transactional
    fun register(request: MemberRegisterRequest) {
        val user = try {
            userRepository.save(
                User(
                    userCiNumber = request.userCiNumber,
                    username = request.userName,
                    phone = request.userPhone,
                    userEmail = request.userEmail,
                    encodedPassword = passwordEncoder.encode(request.password)!!,
                    encodedPin = passwordEncoder.encode(defaultPin)!!,
                )
            )
        } catch (_: DataIntegrityViolationException) {
            throw UserAlreadyExistsException()
        }

        eventPublisher.publish(
            topic = DomainTopics.MEMBER_REGISTERED,
            key = user.getId().toString(),
            eventType = DomainEventTypes.MEMBER_REGISTERED,
            payload = MemberRegisteredEvent(
                userId = user.getId(),
            ),
        )
    }
}
