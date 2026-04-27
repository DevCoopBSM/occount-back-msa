package devcoop.occount.inquiry.application.usecase.password

import devcoop.occount.inquiry.application.exception.PasswordResetTokenAlreadyUsedException
import devcoop.occount.inquiry.application.exception.PasswordResetTokenExpiredException
import devcoop.occount.inquiry.application.exception.PasswordResetTokenNotFoundException
import devcoop.occount.inquiry.application.output.PasswordResetTokenRepository
import org.springframework.stereotype.Service

@Service
class ValidatePasswordResetTokenUseCase(
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
) {
    fun validate(token: String): String {
        val resetToken = passwordResetTokenRepository.findByToken(token)
            ?: throw PasswordResetTokenNotFoundException()

        if (resetToken.used) throw PasswordResetTokenAlreadyUsedException()

        if (resetToken.isExpired()) throw PasswordResetTokenExpiredException()

        passwordResetTokenRepository.save(resetToken.use())

        return resetToken.email
    }
}
