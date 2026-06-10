package devcoop.occount.member.application.usecase.register

import devcoop.occount.core.common.event.DomainEventTypes
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.member.application.event.MemberRegisteredEvent
import devcoop.occount.member.application.exception.EmailNotVerifiedException
import devcoop.occount.member.application.exception.UserAlreadyExistsException
import devcoop.occount.member.application.otp.OtpPurpose
import devcoop.occount.member.application.output.EmailOtpRepository
import devcoop.occount.member.application.output.UserRepository
import devcoop.occount.member.domain.user.User
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegisterUserUseCase(
    private val userRepository: UserRepository,
    private val eventPublisher: EventPublisher,
    private val passwordEncoder: PasswordEncoder,
    private val emailOtpRepository: EmailOtpRepository,
) {
    @Transactional
    fun register(request: MemberRegisterRequest) {
        val emailOtp = emailOtpRepository.findValidByEmail(request.userEmail)
        if (emailOtp == null || !emailOtp.verified || emailOtp.purpose != OtpPurpose.REGISTER) {
            throw EmailNotVerifiedException()
        }

        val user = try {
            userRepository.save(
                User.register(
                    userCiNumber = request.userCiNumber,
                    username = request.username,
                    phone = request.userPhone,
                    email = request.userEmail,
                    encodedPassword = passwordEncoder.encode(request.password)!!,
                    encodedPin = passwordEncoder.encode(request.pin)!!,
                    birthDate = request.birthDate,
                )
            )
        } catch (_: DataIntegrityViolationException) {
            throw UserAlreadyExistsException()
        }

        emailOtpRepository.deleteByEmail(request.userEmail)

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
