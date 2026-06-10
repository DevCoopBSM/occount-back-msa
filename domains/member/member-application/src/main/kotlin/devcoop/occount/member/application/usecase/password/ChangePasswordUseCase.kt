package devcoop.occount.member.application.usecase.password

import devcoop.occount.member.application.exception.EmailNotVerifiedException
import devcoop.occount.member.application.exception.UserNotFoundException
import devcoop.occount.member.application.output.EmailOtpRepository
import devcoop.occount.member.application.output.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChangePasswordUseCase(
    private val userRepository: UserRepository,
    private val emailOtpRepository: EmailOtpRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    @Transactional
    fun changePassword(request: ChangePasswordRequest) {
        val emailOtp = emailOtpRepository.findValidByEmail(request.email)
        if (emailOtp == null || !emailOtp.verified) {
            throw EmailNotVerifiedException()
        }

        val user = userRepository.findByEmail(request.email)
            ?: throw UserNotFoundException()

        userRepository.save(user.changePassword(passwordEncoder.encode(request.newPassword)!!))

        emailOtpRepository.deleteByEmail(request.email)
    }
}
