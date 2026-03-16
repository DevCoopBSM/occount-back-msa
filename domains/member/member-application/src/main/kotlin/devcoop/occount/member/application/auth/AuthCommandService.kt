package devcoop.occount.member.application.auth

import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.member.application.user.UserNotFoundException
import devcoop.occount.member.application.user.UserRepository
import devcoop.occount.member.domain.user.User
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthCommandService(
    private val userRepository: UserRepository,
    private val eventPublisher: EventPublisher,
    private val tokenGenerator: TokenGenerator,
    private val passwordEncoder: PasswordEncoder,
    @param:Value("\${app.default-pin}")
    private val defaultPin: String,
) {
    private companion object {
        const val MEMBER_REGISTERED_EVENT_TYPE = "MemberRegisteredEvent"
    }

    @Transactional
    fun register(request: MemberRegisterRequest) {
        if (userRepository.existsByUserEmail(request.userEmail)) {
            throw UserAlreadyExistsException()
        }

        val user = User(
            userCiNumber = request.userCiNumber,
            username = request.userName,
            phone = request.userPhone,
            userEmail = request.userEmail,
            encodedPassword = passwordEncoder.encode(request.password),
            encodedPin = passwordEncoder.encode(defaultPin),
        )
        val savedUser = userRepository.save(user)

        eventPublisher.publish(
            topic = DomainTopics.MEMBER_REGISTERED,
            key = savedUser.getId().toString(),
            eventType = MEMBER_REGISTERED_EVENT_TYPE,
            payload = mapOf(
                "userId" to savedUser.getId(),
                "email" to request.userEmail,
            ),
        )
    }

    fun login(request: MemberLoginRequest): String {
        val user = userRepository.findByUserEmail(request.userEmail)
            ?: throw UserNotFoundException()

        passwordValidate(request.password, user.getPassword())

        return tokenGenerator.createAccessToken(user.getId(), user.getRole().name)
    }

    fun login(request: KioskLoginRequest): String {
        val user = userRepository.findByUserBarcode(request.userBarcode)
            ?: throw UserNotFoundException()

        passwordValidate(request.userPin, user.getUserPin())

        return tokenGenerator.createKioskToken(user.getId(), user.getRole().name)
    }

    fun passwordValidate(requestPassword: String, expectedPassword: String) {
        if(!passwordEncoder.matches(requestPassword, expectedPassword)) {
            throw InvalidPasswordException()
        }
    }
}
